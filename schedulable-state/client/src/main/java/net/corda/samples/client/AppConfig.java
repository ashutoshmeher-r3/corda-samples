package net.corda.samples.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.core.messaging.CordaRPCOps;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static net.corda.core.utilities.NetworkHostAndPort.parse;

@Configuration
public class RPCConfig {

//    @Value("${partyA.host}")
//    private String partyAHostAndPort;
//
//    @Value("${partyB.host}")
//    private String partyBHostAndPort;
//
//    @Value("${partyC.host}")
//    private String partyCHostAndPort;
//
//    @Bean
//    public CordaRPCOps partyAProxy(){
//        CordaRPCClient partyAClient = new CordaRPCClient(parse(partyAHostAndPort));
//        return partyAClient.start("user1", "test").getProxy();
//    }
//
//    @Bean
//    public CordaRPCOps partyBProxy(){
//        CordaRPCClient partyBClient = new CordaRPCClient(parse(partyBHostAndPort));
//        return partyBClient.start("user1", "test").getProxy();
//    }
//
//    @Bean
//    public CordaRPCOps partyCProxy(){
//        CordaRPCClient partyCClient = new CordaRPCClient(parse(partyCHostAndPort));
//        return partyCClient.start("user1", "test").getProxy();
//    }
//
//    @Bean
//    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(){
//        ObjectMapper mapper =  JacksonSupport.createDefaultMapper(partyAProxy());
//        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
//        converter.setObjectMapper(mapper);
//        return converter;
//    }
}
