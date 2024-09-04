package com.atguigu.gulimall.member;


import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

//@SpringBootTest
public class GulimallMemberApplicationTests {

    @Test
    public void contextLoads() {
        String s = DigestUtils.md5Hex("123456");
        System.out.println(s);
    }

}
