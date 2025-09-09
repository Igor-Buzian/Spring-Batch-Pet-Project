package batch.config;


import batch.interfaces.ScheduledJob;
import entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
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
import processes.UserItemProcessor;

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
    @Autowired
    private final ResourceLoader resourceLoader;

    private final int chunkSize = 1;

    @Value("${spring.batch.userField}")
    private String[] names;

    @Value("${spring.batch.classPathOutput}")
    private String classPathOutput;

    @Value("classpath:sql/user-select.sql")
    private String classpathUserSelect;

    @Value("${spring.time.format}")
    private  String TimeFormat;

    @Bean
    public JdbcCursorItemReader<User> csvReader(DataSource dataSource) throws IOException {
        Resource selectUsers = resourceLoader.getResource(classpathUserSelect);
        String sqlQuery = StreamUtils.copyToString(selectUsers.getInputStream(), StandardCharsets.UTF_8);

        return new JdbcCursorItemReaderBuilder<User>()
                .name("csvReader")
                .dataSource(dataSource)
                .sql(sqlQuery)
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
        File outputDir = outputFile.getParentFile();

        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
        }

        return new FlatFileItemWriterBuilder<User>()
                .name("fileWriterNameComponent")
                .resource(new FileSystemResource(outputFile))
                .delimited()
                .names(names)
                .saveState(false)
                .build();
    }

    private String generateOutputFileName() {
        String date = new SimpleDateFormat(TimeFormat).format(new Date());
        return classPathOutput + "/users-" + date + ".csv";
    }


    @Bean
    public Step stepCsvToDb(@Qualifier("fileItemWriter") ItemWriter<User> writer, DataSource dataSource) throws IOException {
        return new StepBuilder("stepCsvToDb", jobRepository)
                .<User, User>chunk(chunkSize, transactionManager)
                .reader(csvReader(dataSource))
                .processor(csvToDbProcessor())
                .writer(writer)
                .build();
    }

    @Bean
    @ScheduledJob
    public Job exportDbToCsvJob(Step stepCsvToDb) {
        return new JobBuilder("exportDbToCsvJob", jobRepository)
                .start(stepCsvToDb)
                .build();
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public String getClassPathOutput() {
        return classPathOutput;
    }

    public void setClassPathOutput(String classPathOutput) {
        this.classPathOutput = classPathOutput;
    }

    public String getClasspathUserSelect() {
        return classpathUserSelect;
    }

    public void setClasspathUserSelect(String classpathUserSelect) {
        this.classpathUserSelect = classpathUserSelect;
    }

    public String[] getNames() {
        return names;
    }

    public void setNames(String[] names) {
        this.names = names;
    }

    public String getTimeFormat() {
        return TimeFormat;
    }

    public void setTimeFormat(String timeFormat) {
        TimeFormat = timeFormat;
    }
}
