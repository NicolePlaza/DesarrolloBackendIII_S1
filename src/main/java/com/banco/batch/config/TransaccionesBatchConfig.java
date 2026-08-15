package com.banco.batch.config;

import com.banco.batch.model.Transaccion;
import com.banco.batch.processor.TransaccionProcessor;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
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
public class TransaccionesBatchConfig{
    @Bean
    public FlatFileItemReader<Transaccion> transaccionReader() {
        return new FlatFileItemReaderBuilder<Transaccion>()
                .name("transaccionReader")
                .resource(new ClassPathResource("data/transacciones.csv"))
                .linesToSkip(1) // salta el encabezado
                .delimited()
                .names("id", "fecha", "monto", "tipo")
                .fieldSetMapper(fieldSet -> {
                    Transaccion t = new Transaccion();
                    t.setId(fieldSet.readLong("id"));
                    t.setFecha(LocalDate.parse(fieldSet.readString("fecha")));
                    t.setMonto(fieldSet.readDouble("monto"));
                    t.setTipo(fieldSet.readString("tipo"));
                    return t;
                })
                .build();
    }

    @Bean
    public TransaccionProcessor transaccionProcessor(){
        return new TransaccionProcessor();
    }

    @Bean
    public JpaItemWriter<Transaccion> transaccionWriter(EntityManagerFactory emf){
        return new JpaItemWriterBuilder<Transaccion>()
            .entityManagerFactory(emf)
            .build();
    }

    @Bean
    public Step transaccionStep(JobRepository jobRepository,
                                PlatformTransactionManager tx,
                                FlatFileItemReader<Transaccion> reader,
                                TransaccionProcessor processor,
                                JpaItemWriter<Transaccion> writer) {
        return new StepBuilder("transaccionStep", jobRepository)
                .<Transaccion, Transaccion>chunk(10, tx)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(50)
                .skip(Exception.class)
                .build(); 
    }
    
    @Bean
    public Job transaccionesJob(JobRepository jobRepository, Step transaccionStep) {
        return new JobBuilder("transaccionesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(transaccionStep)
                .build();
    }    
}