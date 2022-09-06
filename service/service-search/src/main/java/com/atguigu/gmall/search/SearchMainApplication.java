package com.atguigu.gmall.search;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * 总结：
 *  1）、普通CRUD，写Bean，写接口
 *  2）、复杂CRUD，ElasticsearchRestTemplate 自己调用相关的方法构造复杂的DSL完成功能
 */
@EnableElasticsearchRepositories //开启ES的自动仓库功能
@SpringCloudApplication
public class SearchMainApplication {
    public static void main(String[] args) {
        SpringApplication.run(SearchMainApplication.class,args);
    }
}
