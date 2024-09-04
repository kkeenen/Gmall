package com.atguigu.gulimall;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class test {

    @Resource
    OSSClient ossClient;

    @Test
    public void test01() throws FileNotFoundException {

        InputStream inputStream = new FileInputStream("F:\\desktop\\西郊有密林.jpg");

        ossClient.putObject("gulimall-2398180969", "pic1.jpg", inputStream);

        ossClient.shutdown();
    }
}
