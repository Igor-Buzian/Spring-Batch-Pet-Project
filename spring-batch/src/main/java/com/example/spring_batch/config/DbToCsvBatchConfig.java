package com.example.spring_batch.config;

import com.example.spring_batch.entity.User;
import com.example.spring_batch.processes.UserItemProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Configuration
public class DbToCsvBatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ResourceLoader resourceLoader;

    @Value("${spring.batch.chunkSize}")
    private int chunkSize;

    @Value("${spring.batch.userField}")
    private String[] names;

    @Value("${spring.batch.read.name}")
    private String readerNameComponent;

    @Value("${spring.batch.classPathInput}")
    private String classPathResource;

    @Value("${spring.batch.sql.classpathUserMerge}")
    private String classpathUserMerge;

    @Bean
    public UserItemProcessor dbToCsvProcessor() {
        return new UserItemProcessor();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<User> dbReader() {
        return new FlatFileItemReaderBuilder<User>()
                .name(readerNameComponent)
                .resource(new ClassPathResource(classPathResource))
                .delimited()
                .names(names)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(User.class);
                }})
                .saveState(false)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<User> databaseItemWriter(DataSource dataSource) throws IOException {
        Resource userMergeSql = resourceLoader.getResource(classpathUserMerge);
        String sqlQuery = StreamUtils.copyToString(userMergeSql.getInputStream(), StandardCharsets.UTF_8);

        return new JdbcBatchItemWriterBuilder<User>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql(sqlQuery)
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public Step stepDbToFile(@Qualifier("databaseItemWriter") ItemWriter<User> writer) {
        return new StepBuilder("stepDbToFile", jobRepository)
                .<User, User>chunk(chunkSize, transactionManager)
                .reader(dbReader())
                .processor(dbToCsvProcessor())
                .writer(writer)
                .build();
    }

    @Bean
    public Job importCsvToDbJob(Step stepDbToFile) {
        return new JobBuilder("importCsvToDbJob", jobRepository)
                .start(stepDbToFile)
                .build();
    }
}
