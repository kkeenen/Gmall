package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.feign.ProductFeignService;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.AttrRespVo;
import com.atguigu.gulimall.search.vo.BrandVo;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParam param) {
        // 1 动态
        SearchResult result = null;
        // 1.准备检索请求
        SearchRequest searchRequest = buildSearchRequest(param);

        try {
            // 2. 执行检索请求
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

            // 3. 封装
            result = buildSearchResult(response, param);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 模糊匹配，过滤，（按照属性，分类，品牌，价格区间，库存）
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //1.1 must 模糊匹配
        if(!StringUtils.isEmpty(param.getKeyword())){
            boolQuery.must(QueryBuilders.matchQuery("skuTitle",param.getKeyword()));
        }
        //1.2 bool -filter 按照三级分类id查询
        if(param.getCatalog3Id() != null){
            boolQuery.filter(QueryBuilders.termQuery("catalogId",param.getCatalog3Id()));
        }
        //1.3 bool -filter 按照品牌id查询
        if(param.getBrandId() != null &&param.getBrandId().size() != 0){
            boolQuery.filter(QueryBuilders.termsQuery("brandId",param.getBrandId()));
        }
        //1.4bool -filter 按照是否有库存查询
        if(param.getHasStock() != null){
            boolQuery.filter(QueryBuilders.termQuery("hasStock",param.getHasStock()==1));
        }

        // 1.5 bool -filter 按照价格区间查询
        if(!StringUtils.isEmpty(param.getSkuPrice())) {

            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 2) {
                if (s[0] != null)
                    rangeQuery.gte(s[0]).lte(s[1]);
                else {
                    rangeQuery.lte(s[1]);
                }
            } else rangeQuery.gte(s[0]);
            boolQuery.filter(rangeQuery);
        }

        // 1.6 bool -filter 按照属性查询
        if(param.getAttrs() != null && param.getAttrs().size() > 0){
            for (String attrStr : param.getAttrs()) {
                BoolQueryBuilder nestedbollQuery = QueryBuilders.boolQuery();
                String[] s = attrStr.split("_");
                String attrId = s[0];
                String[] attrValues = s[1].split(":");
                nestedbollQuery.must(QueryBuilders.termQuery("attrs.attrId",attrId));
                nestedbollQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));

                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedbollQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }

        // 封装以上所有条件
        sourceBuilder.query(boolQuery);

        //2排序 分页 高亮
        // 2.1 排序
        if(!StringUtils.isEmpty(param.getSort())){
            String sort = param.getSort();
            // sort=hotScore_asc/desc
            String[] s = sort.split("_");
            if(s[1].equalsIgnoreCase("asc")){
                sourceBuilder.sort(s[0], SortOrder.ASC);
            }else {
                sourceBuilder.sort(s[0], SortOrder.DESC);
            }
        }

        // 2.2 分页
        sourceBuilder.from((param.getPageNum()-1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        // 2.3 高亮
        if(!StringUtils.isEmpty(param.getKeyword())){
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            sourceBuilder.highlighter(highlightBuilder);
        }

        //3聚合分析
        // 聚合品牌
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);

        // 品牌聚合子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        // TODO : 1 聚合brand
        sourceBuilder.aggregation(brand_agg);
        // 分类聚合!!!!!
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        // TODO : 2 聚合catalog
        sourceBuilder.aggregation(catalog_agg);

        // 属性聚合
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        // 聚合当前所有attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        // 聚合分析出当前attrId对应的名字
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        // 聚合当前attrId对应的可能的属性值attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        // 放入聚合中
        attr_agg.subAggregation(attr_id_agg);
        // TODO : 3 聚合属性
        sourceBuilder.aggregation(attr_agg);


        String s = sourceBuilder.toString();
//        System.out.println("构建的DSL ： " + s);

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_IDDEX}, sourceBuilder);
        return searchRequest;
    }

    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {

        SearchResult result = new SearchResult();
        // 1返回的所有查询到的商品
        SearchHits hits = response.getHits();
        List<SkuEsModel> esModels = new ArrayList<>();
        if(hits.getHits() != null && hits.getHits().length>0){
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if(!StringUtils.isEmpty(param.getKeyword())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].toString();
                    esModel.setSkuTitle(string);
                }
                esModels.add(esModel);
            }
        }
        result.setProducts(esModels);

        // 2当前所有商品设计的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            // 属性id
            long attrId = bucket.getKeyAsNumber().longValue();
            // 属性名字
            ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attr_name_agg.getBuckets().get(0).getKeyAsString();
            // 属性所有值
            List<String> attrValues = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = item.getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());
            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValues);

            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);

        // 3当前所有商品设计的所有品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            // 品牌id
            long brandId = bucket.getKeyAsNumber().longValue();
            // 品牌名字
            String brandName = ((ParsedStringTerms) bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();
            // 品牌图片
            String brandImg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();
            brandVo.setBrandId(brandId);
            brandVo.setBrandImg(brandImg);
            brandVo.setBrandName(brandName);
            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);

        // 4当前所有商品设计的所有分类信息
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");

        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            // 得到分类id
            String keyAsString = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));
            // 得到分类名
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        // 5 分页信息-页码

        // 5 分页信息-总记录数
        result.setPageNum(param.getPageNum());
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        // 5 分页信息-总页码
        int totalPages = total % EsConstant.PRODUCT_PAGESIZE == 0 ? (int)total % EsConstant.PRODUCT_PAGESIZE:((int)total % EsConstant.PRODUCT_PAGESIZE)+1;
        result.setTotalPages(totalPages);

        List<Integer> pageNums = new ArrayList<>();
        for(int i=1 ; i<=totalPages ; i++){
            pageNums.add(i);
        }
        result.setPageNavs(pageNums);

        // 构建面包屑
        if(param.getAttrs() != null && param.getAttrs().size()>0){
            List<SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
                // 分析每一个attr传过来的参数值
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                R r = productFeignService.getAttrInfo(Long.parseLong(s[0]));
                result.getAttrIds().add(Long.parseLong(s[0]));
                if (r.getCode() == 0) {
                    AttrRespVo data = r.getData("attr", new TypeReference<AttrRespVo>() {});
                    navVo.setNavName(data.getAttrName());
                }else {
                    navVo.setNavName(s[0]);
                }
                // 取消面包屑之后要跳转   将请求地址的url里面的当前置空
                // 拿到所有条件 去掉当前
                String replace = replaceQueryString(param, attr, "attrs");
                navVo.setLink("http://search.dns19.hichina.com/list.html?" + replace);

                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(collect);

        }
        // 品牌分类
        if(param.getBrandId() != null && param.getBrandId().size()>0){
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();

            navVo.setNavName("品牌");
            // TODO 远程查询所有品牌
            R r = productFeignService.brandsinfo(param.getBrandId());
            if(r.getCode()==0){
                List<BrandVo> brand = r.getData("brand", new TypeReference<List<BrandVo>>(){});
                StringBuffer buffer = new StringBuffer();
                String replace = "";
                for (BrandVo brandVo : brand) {
                    buffer.append(brandVo.getBrandName()+";");
                    replace = replaceQueryString(param, brandVo.getBrandId()+"","brandId");
                }
                navVo.setNavValue(buffer.toString());
                navVo.setLink("http://search.dns19.hichina.com/list.html?" + replace);
            }
            navs.add(navVo);
        }




        return result;
    }

    private static String replaceQueryString(SearchParam param, String value, String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            encode = encode.replace("+","%20"); //浏览器对空格编码和java不一样
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String replace = param.get_queryString().replace("&"+key+"=" + encode, "");
        return replace;
    }


}
