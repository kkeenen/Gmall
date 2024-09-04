package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    private StringRedisTemplate redisTemplate;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     *
     * 查出所有分类 以树返回
     * @return
     */
    @Override
    public List<CategoryEntity> listWithTree() {
        //1. 查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2.
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
            categoryEntity.getParentCid() == 0
        ).map((menu)->{
            menu.setChildren(getChildren(menu,entities));
            return menu;
        }).sorted((menu1, menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> list) {
        // TODO // 检查当前删除的菜单是否被其他地方引用
        // 逻辑删除
        baseMapper.deleteBatchIds(list);

    }

    @Override
    public Long[] findCategoryPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        Collections.reverse(parentPath);
        return paths.toArray(new Long[parentPath.size()]);
    }

    // 级联更新所有关联的数据


//    @Caching( evict = {
//            @CacheEvict(value = "category", key = "'getLevel_1'"),
//            @CacheEvict(value = "category", key = "'getCatelogJson'")
//    })
    @CacheEvict(value = "category", allEntries = true)
    @Override
    @Transactional
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }

    @Cacheable(value = {"category"} , key = "#root.method.name", sync = true)
    @Override
    public List<CategoryEntity> getLevel_1() {
        System.out.println("get level 1 categorys");
        List<CategoryEntity> entities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("cat_level", 1));
        return entities;

    }

    @Override
    @Cacheable(value = "category", key = "#root.method.name", sync = true)
    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        System.out.println("查询了数据库.....");

        List<CategoryEntity> selectList = baseMapper.selectList(null);
        // 查出所有1级分类
        List<CategoryEntity> level1 = getParent_cid(selectList, 0L);
        // 封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 对于每一个一级分类，查到它的二级分类
            List<CategoryEntity> level2 = getParent_cid(selectList,v.getCatId());
            // 封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (level2 != null) {
                catelog2Vos = level2.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    // 找当前二级分类的三级分类 封装成vo
                    List<CategoryEntity> level3 = getParent_cid(selectList, l2.getCatId());
                    if(level3 != null){
                        List<Catelog2Vo.Catelog3Vo> collect = level3.stream().map(l3 -> {
                            // 封装成指定格式
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        return parent_cid;

    }

    public Map<String, List<Catelog2Vo>> getCatelogJson2() {

        // 1 加入缓存逻辑  约定缓存中存json字符串，好处是 跨语言跨平台兼容
        String json = redisTemplate.opsForValue().get("catelogJSON");
        if(StringUtils.isEmpty(json)){
            System.out.println("缓存不命中....查询数据库.....");
            // 缓存中没有 去查数据库
            Map<String, List<Catelog2Vo>> catelogJsonFromDB = getCatelogJsonFromDBWithRedisLock();
            return catelogJsonFromDB;
        }
        System.out.println("缓存命中...直接返回");
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(json, new TypeReference<Map<String,List<Catelog2Vo>>>(){});
        return result;

    }

    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDBWithRedisLock() {

        // 占分布式锁 去redis占坑
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid,300,TimeUnit.SECONDS);
        if(lock) {
            System.out.println("获取分布式锁成功");
            // 加锁成功...执行业务
            // 设置过期时间  如果来不及删掉锁就会死锁,必须和加锁是同步的
//            redisTemplate.expire("lock",30,TimeUnit.SECONDS);
            Map<String, List<Catelog2Vo>> dataFromDB;
            try {
                dataFromDB = getDataFromDB();
            } finally {
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                // 删除锁 可以保证原子性
                Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                        Arrays.asList("lock"), uuid);
            }
            return dataFromDB;
            // 删锁操作必须获取uuid操作是原子操作， 使用Lua脚本解锁
//            redisTemplate.delete("lock");
//            String lockValue = redisTemplate.opsForValue().get("lock");
//            if(lockValue.equals(uuid)){
//                redisTemplate.delete("lock");
//            }


        }else {
            // 加锁失败...重试
            // 休眠100ms重试
            System.out.println("获取分布式锁失败，等待重试");
            try{
                Thread.sleep(200);
            }catch (Exception e){

            }
            return getCatelogJsonFromDBWithRedisLock(); // 自选
        }




    }

    private Map<String, List<Catelog2Vo>> getDataFromDB() {
        // 查看缓存是否存在
        String json = redisTemplate.opsForValue().get("catelogJSON");
        if(!StringUtils.isEmpty(json)){
            // 缓存不为空直接返回
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(json, new TypeReference<Map<String,List<Catelog2Vo>>>(){});
            return result;
        }
        System.out.println("查询了数据库.....");

        List<CategoryEntity> selectList = baseMapper.selectList(null);

        // 查出所有1级分类
        List<CategoryEntity> level1 = getParent_cid(selectList, 0L);
        // 封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 对于每一个一级分类，查到它的二级分类
            List<CategoryEntity> level2 = getParent_cid(selectList,v.getCatId());
            // 封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (level2 != null) {
                catelog2Vos = level2.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    // 找当前二级分类的三级分类 封装成vo
                    List<CategoryEntity> level3 = getParent_cid(selectList, l2.getCatId());
                    if(level3 != null){
                        List<Catelog2Vo.Catelog3Vo> collect = level3.stream().map(l3 -> {
                            // 封装成指定格式
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        // 把查到的数据放入缓存
        // 把查到的对象转为json再放入缓存
        String jsonString = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catelogJSON",jsonString,1, TimeUnit.DAYS);

        return parent_cid;
    }

    // 从数据库查询并封装分类数据，不使用缓存 使用本地锁
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDBWithLocalLock() {

        synchronized (this){
            // 查看缓存是否存在
            return getDataFromDB();
        }


    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
        return collect;
//        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
    }

    List<Long> findParentPath(Long catelogId, List<Long> paths){
        paths.add(catelogId);
        CategoryEntity categoryEntity = this.getById(catelogId);
        if(categoryEntity.getParentCid() != 0){
            findParentPath(categoryEntity.getParentCid(), paths);
        }
        return paths;
    }

    // 递归查找所有菜单子菜单
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all){

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            categoryEntity.setChildren(getChildren(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());
        return children;
    }

}





