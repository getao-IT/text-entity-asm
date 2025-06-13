package com.iecas.evaluate.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iecas.evaluate.pojo.entity.TextIdentifyEntity;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.List;


@SpringBootTest
public class FunTest {

    @Test
    public void testGetFileContentToListEntity() throws IOException {
        String filePath = "D:\\iecas\\开发相关文档\\黄寺\\deploydata\\模型运行与评估数据\\assessment\\test_entity\\output\\中国成立了.txt";
        ObjectMapper objectMapper = new ObjectMapper();
        List<TextIdentifyEntity> entities = objectMapper.readValue(new File(filePath), objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, TextIdentifyEntity.class));
        for (TextIdentifyEntity entity : entities) {
            System.out.println(entity.toString());
        }
    }
}
