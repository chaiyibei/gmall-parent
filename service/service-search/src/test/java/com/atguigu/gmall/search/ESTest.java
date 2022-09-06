package com.atguigu.gmall.search;

import com.atguigu.gmall.search.bean.Person;
import com.atguigu.gmall.search.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

import java.util.List;
import java.util.Optional;

@SpringBootTest
public class ESTest {
    @Autowired
    PersonRepository personRepository;

    @Autowired
    ElasticsearchRestTemplate esRestTemplate;

    @Test
    public void testSave(){
        Person person = new Person();
        Person person1 = new Person();
        Person person2 = new Person();
        Person person3 = new Person();

        person1.setId(3l);
        person1.setFirstName("四");
        person1.setLastName("李");
        person1.setAge(20);
        person1.setAddress("西安市雁塔区");

        person2.setId(2l);
        person2.setFirstName("三");
        person2.setLastName("张");
        person2.setAge(19);
        person2.setAddress("西安市碑林区");

        person3.setId(4l);
        person3.setFirstName("五");
        person3.setLastName("王");
        person3.setAge(21);
        person3.setAddress("北京市朝阳区");

        personRepository.save(person1);
        personRepository.save(person2);
        personRepository.save(person3);
        System.out.println("新增完成。。。");

    }

    @Test
    public void testQuery(){
//        Optional<Person> person = personRepository.findById(1l);
//        System.out.println(person.get());

        //1、查询 address 在西安市的人
        List<Person> all = personRepository.findAllByAddressLike("西安市");
        for (Person person : all) {
            System.out.println(person);
        }
    }
    
    @Test
    public void testFindPerson(){
        List<Person> 北京市 = personRepository.findPerson("北京市");
        for (Person person : 北京市) {
            System.out.println(person);
        }
    }
}
