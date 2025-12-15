package com.example.ShardedSagaWallet.saga;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Builder
@Getter
public class SagaContext {
    private Map<String,Object> data;

    public SagaContext() {
        // required by Jackson
    }


    public SagaContext(Map<String, Object> data){
        this.data = data != null ? data : new HashMap<>();
    }

    public void put(String key, Object value){
        data.put(key,value);
    }

    public Object get(String key){
        return data.get(key);
    }

    public Long getLong(String key){
        Object value = data.get(key);
        if(value instanceof Number){
            return ((Number) value).longValue();
        }
        return null;
    }

    public BigDecimal getBigDecimal(String key){
        Object value = data.get(key);
        if(value instanceof Number){
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return null;
    }
}
