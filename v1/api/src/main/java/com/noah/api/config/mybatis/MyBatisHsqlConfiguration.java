package com.noah.api.config.mybatis;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@MapperScan(value = "com.noah.api.app.person.service.mapper.PersonMapper", sqlSessionFactoryRef = "hSqlSessionFactory")
public class MyBatisHsqlConfiguration {

    @Primary
    @Bean(name = "hSqlSessionFactory")
    public SqlSessionFactory mt20SqlSessionFactory( @Qualifier("hSqlDataSource") DataSource dataSource, ApplicationContext applicationContext) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setTypeAliasesPackage("com.noah.api.app.person.entity.**");
        sqlSessionFactoryBean.setMapperLocations(applicationContext.getResources("classpath:mapper/app/person/*Sqlmap.xml")); // resources 위치의 경로

        // MyBatis Configuration 객체 생성
        org.apache.ibatis.session.Configuration mybatisConfig = new org.apache.ibatis.session.Configuration();
        mybatisConfig.setMapUnderscoreToCamelCase(true); // mapUnderscoreToCamelCase 설정을 true로 설정
        sqlSessionFactoryBean.setConfiguration(mybatisConfig); // SqlSessionFactoryBean에 MyBatis Configuration 설정

        log.info("hSqlSessionFactory...called");
        return sqlSessionFactoryBean.getObject();
    }

    @Bean(name = "hSqlSessionTemplate")
    public SqlSessionTemplate mt20SqlSessionTemplate( @Qualifier("hSqlSessionFactory") SqlSessionFactory sqlSessionFactory ) {
    	log.info("hSqlSessionTemplate...called");
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
