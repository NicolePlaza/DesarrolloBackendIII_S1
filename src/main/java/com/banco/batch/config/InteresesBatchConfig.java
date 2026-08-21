package com.banco.batch.config;

import com.banco.batch.model.CuentaInteres;
import com.banco.batch.processor.InteresProcessor;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class InteresesBatchConfig {

    @Bean
    public FlatFileItemReader<CuentaInteres> interesReader() {
        return new FlatFileItemReaderBuilder<CuentaInteres>()
                .name("interesReader")
                .resource(new ClassPathResource("data/intereses.csv"))
                .linesToSkip(1)
                .delimited()
                .names("cuenta_id", "nombre", "saldo", "edad", "tipo")
                .fieldSetMapper(fieldSet -> {
                    CuentaInteres c = new CuentaInteres();
                    c.setCuentaId(fieldSet.readLong("cuenta_id"));
                    c.setNombre(fieldSet.readString("nombre"));
                    c.setSaldo(fieldSet.readDouble("saldo"));
                    c.setEdad(fieldSet.readInt("edad"));
                    c.setTipo(fieldSet.readString("tipo"));
                    return c;
                })
                .build();
    }

    @Bean
    public InteresProcessor interesProcessor() {
        return new InteresProcessor();
    }

    @Bean
    public JpaItemWriter<CuentaInteres> interesWriter(EntityManagerFactory emf) {
        return new JpaItemWriterBuilder<CuentaInteres>()
                .entityManagerFactory(emf)
                .build();
    }

    @Bean
    public Step interesStep(JobRepository jobRepository,
                             PlatformTransactionManager tx,
                             FlatFileItemReader<CuentaInteres> interesReader,
                             InteresProcessor interesProcessor,
                             JpaItemWriter<CuentaInteres> interesWriter) {
        return new StepBuilder("interesStep", jobRepository)
                .<CuentaInteres, CuentaInteres>chunk(10, tx)
                .reader(interesReader)
                .processor(interesProcessor)
                .writer(interesWriter)
                .faultTolerant()
                .skipLimit(50)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Job interesesJob(JobRepository jobRepository, Step interesStep) {
        return new JobBuilder("interesesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(interesStep)
                .build();
    }
}
