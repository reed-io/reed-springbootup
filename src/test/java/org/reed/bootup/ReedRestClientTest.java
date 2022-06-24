package org.reed.bootup;

import org.junit.Test;
import org.reed.restful.ReedRestClient;

/**
 * ReedRestClientTest Create on 2019-08-06
 * <p>
 * Copyright (c) 2019-08-06 reed, All rights reserved.
 *
 * @author 田广文
 * @version 1.0
 */
public class ReedRestClientTest {
    @Test
    public void testGetCheckToken()
    {
        ReedRestClient restClient = new ReedRestClient();
        restClient.setConnectTimeout(1000);
        restClient.setReadTimeout(1000);
//        ReedResult result = restClient.get("Auth", "checkToken/");
//        Assert.assertEquals(result.getCode(), 0);
    }
}
