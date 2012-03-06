/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.cluster.merger;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ResultMergerTest {

    @Test
    public void testListMerger() throws Exception {
        List<Object> list1 = new ArrayList<Object>();
        list1.add( null );
        list1.add( "1" );
        list1.add( "2" );
        List<Object> list2 = new ArrayList<Object>();
        list2.add( "3" );
        list2.add( "4" );
        
        List result = ListMerger.INSTANCE.merge( list1, list2 );
        Assert.assertEquals(5, result.size());
        Assert.assertEquals( new ArrayList<String>(){
            {
                add( null );
                add( "1" );
                add( "2" );
                add( "3" );
                add( "4" );
            }
        }, result);
    }
    
    @Test
    public void testSetMerger() throws Exception {
        Set<Object> set1 = new HashSet<Object>();
        set1.add( null );
        set1.add( "1" );
        set1.add( "2" );
        Set<Object> set2 = new HashSet<Object>();
        set2.add( "2" );
        set2.add( "3" );
        
        Set result = SetMerger.INSTANCE.merge( set1, set2 );
        
        Assert.assertEquals( 4, result.size() );
        Assert.assertEquals( new HashSet<String>(){
            {
                add( null );
                add( "1" );
                add( "2" );
                add( "3" );
            }
        }, result);
    }

}
