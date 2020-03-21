package com.leaf.example.demo.generic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloServiceImpl implements HelloService {

    /**
     * logger
     */
    private final static Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);

    @Override
    public String sayHello(String name, String age) {
        logger.info("HelloServiceImpl name:{}, age:{}", name, age);
        return "hello" + name;
    }

    @Override
    public String sayHello(User user) {
        logger.info("HelloServiceImpl param:{}", user.getName());
        return null;
    }
}
