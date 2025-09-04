package com.example.spring_batch.config;

import com.example.spring_batch.entity.User;
import com.example.spring_batch.processes.UserItemProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

@Configuration
public class BatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Value("${spring.batch.chunkSize}")
    private int chunkSize;

    @Value("${spring.batch.userField}")
    private String[] names;

    @Value("${spring.batch.read.name}")
    private String readerNameComponent;

    @Value("${spring.batch.classPathInput}")
    private String classPathResource;

    @Value("${spring.batch.write.name}")
    private String writerNameComponent;

    @Value("${spring.batch.classPathOutput}")
    private String classPathOutput;

    @Value("${spring.batch.sql.classpathUserMerge}")
    private String classpathUserMerge;

    /*@Value("${spring.batch.sql.userSelect}")
    private String userSelect;*/
    private final ResourceLoader resourceLoader;


    public BatchConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager, ResourceLoader resourceLoader) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.resourceLoader = resourceLoader;
    }

    @Bean
    @StepScope
    public FlatFileItemReader<User> reader() {
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

/*@Bean
@StepScope
public JdbcCursorItemReader<User> reader(DataSource dataSource) {
    return new JdbcCursorItemReaderBuilder<User>()
            .name("userDbReader")
            .dataSource(dataSource)
            .sql("SELECT id, name, email FROM users") // адаптируй под свою таблицу и поля
            .rowMapper(new BeanPropertyRowMapper<>(User.class))
            .build();
}*/
    @Bean
    public UserItemProcessor processor() {
        return new UserItemProcessor();
    }

    @Bean
    public ItemWriter<User> consoleItemWriter() {
        return users -> users.forEach(System.out::println);
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<User> fileItemWriter() {
        String outputFileName = generateOutputFileName();
        File outputFile = new File(outputFileName);
        File parentDir = outputFile.getParentFile();

        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        return new FlatFileItemWriterBuilder<User>()
                .name(writerNameComponent)
                .resource(new FileSystemResource(outputFile))
                .delimited()
                .names(names)
                .saveState(false)
                .build();
    }

    private String generateOutputFileName() {
        String date = new SimpleDateFormat("yyyyMMdd-HHmmssSSS").format(new Date());
        return classPathOutput +"/users-"+date+".csv";
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
    public Step step1(@Qualifier("fileItemWriter") ItemWriter<User> writer) {
        return new StepBuilder("step1", jobRepository)
                .<User, User>chunk(chunkSize, transactionManager)
                .reader(reader())
                .processor(processor())
                .writer(writer)
                .build();
    }

    @Bean
    public Job importUserJob(Step step1) {
        return new JobBuilder("importUserJob", jobRepository)
                .start(step1)
                .build();
    }
}