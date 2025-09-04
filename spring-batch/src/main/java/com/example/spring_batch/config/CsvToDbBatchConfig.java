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
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@RequiredArgsConstructor
public class CsvToDbBatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ResourceLoader resourceLoader;

    @Value("${spring.batch.chunkSize}")
    private int chunkSize;

    @Value("${spring.batch.userField}")
    private String[] names;

    @Value("${spring.batch.write.name}")
    private String writerNameComponent;

    @Value("${spring.batch.classPathOutput}")
    private String classPathOutput;

    @Value("${spring.batch.sql.classpathUserMerge}")
    private String classpathUserMerge;

    @Bean
    @StepScope
    public JdbcCursorItemReader<User> csvReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<User>()
                .name("csvReader")
                .dataSource(dataSource)
                .sql("SELECT id, first_name AS firstName, last_name AS lastName FROM users")
                .rowMapper(new BeanPropertyRowMapper<>(User.class))
                .build();
    }

    @Bean
    public UserItemProcessor csvToDbProcessor() {
        return new UserItemProcessor();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<User> fileItemWriter() {
        String outputFileName = generateOutputFileName();
        File outputFile = new File(outputFileName);

        if (!outputFile.exists()) {
            outputFile.mkdirs();
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
        String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS").format(new Date());
        return classPathOutput + "/users-" + date + ".csv";
    }

    @Bean
    public Step stepCsvToDb(@Qualifier("fileItemWriter") ItemWriter<User> writer, DataSource dataSource) {
        return new StepBuilder("stepCsvToDb", jobRepository)
                .<User, User>chunk(chunkSize, transactionManager)
                .reader(csvReader(dataSource))
                .processor(csvToDbProcessor())
                .writer(writer)
                .build();
    }

    @Bean
    public Job exportDbToCsvJob(Step stepCsvToDb) {
        return new JobBuilder("exportDbToCsvJob", jobRepository)
                .start(stepCsvToDb)
                .build();
    }
}
