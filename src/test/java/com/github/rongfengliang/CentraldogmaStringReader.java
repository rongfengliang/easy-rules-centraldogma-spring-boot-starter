package com.github.rongfengliang;


import com.linecorp.centraldogma.client.CentralDogma;
import com.linecorp.centraldogma.client.armeria.ArmeriaCentralDogmaBuilder;
import com.linecorp.centraldogma.common.Entry;
import com.linecorp.centraldogma.common.Query;
import com.linecorp.centraldogma.common.Revision;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;

public class CentraldogmaStringReader {

    private  CentralDogma centralDogma;
    @Before
    public void  init() throws UnknownHostException {
        centralDogma = new ArmeriaCentralDogmaBuilder()
                .host("127.0.0.1",36462)
                .build();
    }
    @Test
    public   void reader(){
        CompletableFuture<Entry<String>> future =
                centralDogma.getFile("demo", "demo", Revision.HEAD, Query.ofText("/a.txt"));

        StringReader stringReader = new StringReader(future.join().content());
        System.out.println(future.join().content());
    }
}
