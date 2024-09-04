package com.atguigu.gulimall.search;

//import org.junit.jupiter.api.Test;
import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.UUID;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GulimallSearchApplicationTests {

	@Autowired
	RestHighLevelClient client;

	@Autowired
	StringRedisTemplate stringRedisTemplate;

	@Test
	public void testRedis(){
		ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
		ops.set("hello","world"+ UUID.randomUUID().toString());
		String hello = ops.get("hello");
		System.out.println(hello);

	}

	@Test
	public void test01() {
		System.out.println(client);
	}

	@Data
	class User{
		private String username;
		private String gender;
		private Integer age;
	}

	@Data
	static public class Account
	{
		private int account_number;private int balance;private String firstname;private String lastname;private int age;private String gender;private String address;private String employer;private String email;private String city;private String state;
	}

	@Test
	public void searchData() throws IOException {
		//1 创建检索请求
		SearchRequest searchRequest = new SearchRequest();
		// 指定索引
		searchRequest.indices("bank");
		// 指定DSL，检索条件
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		// 1.1 构造检索条件
		sourceBuilder.query(QueryBuilders.matchQuery("address","mill"));
		//1.2 按照年龄的值进行聚合
		TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
		sourceBuilder.aggregation(ageAgg);
		//1.3 计算平均薪资
		AvgAggregationBuilder balanceAgg = AggregationBuilders.avg("balanceAvg").field("balance");
		sourceBuilder.aggregation(balanceAgg);


		System.out.println(sourceBuilder.toString());
		searchRequest.source(sourceBuilder);

		//2 执行检索
		SearchResponse searchResponse = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
		SearchHits hits = searchResponse.getHits();
		SearchHit[] searchHits = hits.getHits();
		for (SearchHit hit : searchHits) {
//			hit.getIndex();hit.getId();
			String sourceAsString = hit.getSourceAsString();
			Account account = JSON.parseObject(sourceAsString, Account.class);
			System.out.println("account:" + account);
		}
		System.out.println(searchResponse);

		// 获取分析信息
		Aggregations aggregations = searchResponse.getAggregations();
		for(Aggregation aggregation : aggregations){

			System.out.println("当前聚合：" + aggregation.getName());

		}
		Terms ageAgg1 = aggregations.get("ageAgg");


	}

	/**
	 * 索引
	 * 更新
	 * client.index
	 * @throws IOException
	 */
	@Test
	public void indexData() throws IOException {
		IndexRequest indexRequest = new IndexRequest("users");
		indexRequest.id("1");
		User user = new User();
		user.setUsername("zhangsan");
		user.setAge(18);
		user.setGender("男");
		String jsonString = JSON.toJSONString(user);
		indexRequest.source(jsonString, XContentType.JSON);

		// 执行操作
		IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
		System.out.println(index);
	}



}
