package com.changhoward.cia10108springboot.howard.rentalorder.service.impl;

import com.changhoward.cia10108springboot.Entity.Rental;
import com.changhoward.cia10108springboot.howard.rentalorder.dao.RentalRepository;
import com.changhoward.cia10108springboot.howard.rentalorder.service.RentalCartService;
import com.changhoward.cia10108springboot.howard.util.JedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RentalCartServiceImpl implements RentalCartService {

    JedisPool jedisPool = null;
    public RentalCartServiceImpl() {
        jedisPool = JedisUtil.getJedisPool();
    }

    @Autowired
    private RentalRepository rentalRepository;

    public Map<String, Map<String, String>> getFromCart(String memNo) {
       /*
        * new 一個叫做 Results 的 map，裡面每一個 key 值代表一個購物車商品存進 Redis 用的 key，這個 key 的值是一個 map，
        * 每一個 map 代表該商品的資訊，例如 : 價格、顏色...
       */
        Map<String, Map<String, String>> results = new HashMap<>();

        try (Jedis jedis = jedisPool.getResource()) {

            jedis.select(1);
            String cursor = "0";

            do {
                // 對下方要進行的掃描做配置，去掃描任何 key 是以 "member : "+memNo+" : item : " 型式的資料，一次回傳100個左右
                ScanParams scanParams = new ScanParams().match("member : " + memNo + " : cartItem : *").count(100);
                // 使用當前 cursor 和掃描參數執行 scan 方法，返回一個 ScanResult 物件，裡面包含執行完 scan 的新的 cursor 值和找到的 key 值(可能不只一個)
                ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
                // 從 ScanResult 取出查詢到的 key 值們，然後放進 List 裡
                List<String> keys = scanResult.getResult();
                // 每次掃描完 cursor 值會更動，取得更動完的 cursor 值，表示下一次要從哪個值開始
                cursor = scanResult.getCursor();

                // 用裝著所有 key 值的 List 去跑迴圈
                for (String key : keys) {
                   /*
                    * 用每一個 key 值取得所有 value (ex : rentalPrice、rentalInfo...)，然後裝進 Map 裡面，
                    * 所以現在這個叫做 item 的 Map，裡面就是"同一個商品"的很多筆下面這樣的資料 :
                    * rentalPrice : 20000、rentalColor : 白色...
                   */
                    Map<String, String> item = jedis.hgetAll(key);
                    // 把每一個代表一個購物車商品的 key 跟對應的 value -> 裝著商品資訊的 Map 放進 result 這個 Map
                    results.put(key, item);
                }
                // cursor 值回到 0，表示已掃描完所有
            } while (!cursor.equals("0"));

        }
        // 回傳裝有所有購物車商品，跟個別商品的所有資訊的 Map
        return results;

    }

    public void setToCart(Integer memNo, Map<String, String> map) {

        try (Jedis jedis = jedisPool.getResource()) {

            jedis.select(1);
            String itemKey = "member : " + memNo + " : cartItem : " + map.get("rentalNo");
            jedis.hmset(itemKey, map);

        }

    }

    public void deleteFromCart(Integer memNo, Integer rentalNo) {

        try(Jedis jedis = jedisPool.getResource()) {

            jedis.select(1);


        }

    }


}