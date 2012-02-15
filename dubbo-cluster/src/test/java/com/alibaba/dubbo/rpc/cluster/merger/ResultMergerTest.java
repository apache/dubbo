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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.rpc.cluster.Merger;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ResultMergerTest {

    @Test
    public void testCollectionMerger() throws Exception {

        Merger merger = ExtensionLoader.getExtensionLoader( Merger.class )
                .getExtension( CollectionMerger.NAME );

        List<String> r1 = new ArrayList<String>();
        r1.add( "1" );
        r1.add( "2" );
        List<String> r2 = new ArrayList<String>();
        r2.add( "3" );
        r2.add( "4" );
        
        Assert.assertNull( merger.merge( null, null ) );
        Assert.assertEquals( r1, merger.merge( r1, null ) );
        Assert.assertEquals( r2, merger.merge( null, r2 ) );
        List<String> all = new ArrayList<String>( r1 );
        all.addAll( r2 );
        Assert.assertEquals( all, merger.merge( r1, r2 ) );

        Set<String> s1 = new HashSet<String>();
        Set<String> s2 = new HashSet<String>();
        s1.add( "1" );
        s1.add( "2" );
        s2.add( "3" );
        s2.add( "4" );
        
        Assert.assertNull( merger.merge( null, null ) );
        Assert.assertEquals( s1, merger.merge( s1, null ) );
        Assert.assertEquals( s2, merger.merge( null, s2 ) );
        Set<String> set = new HashSet<String>();
        set.addAll( s1 );
        set.addAll( s2 );
        Assert.assertEquals( set, merger.merge( s1, s2 ) );

    }
    
    @Test
    public void testMapMerger() throws Exception {
        
        Merger merger = ExtensionLoader.getExtensionLoader( Merger.class )
                .getExtension( MapMerger.NAME );
        
        Map<String, String> m1 = new HashMap<String, String>();
        Map<String, String> m2 = new HashMap<String, String>();
        Map<String, String> map = new HashMap<String, String>();
        m1.put( "1", "1" );
        m1.put( "2", "2" );
        m2.put( "3", "3" );
        m2.put( "4", "4" );
        
        map.putAll( m1 );
        map.putAll( m2 );
        
        Assert.assertNull( merger.merge( null, null ) );
        Assert.assertEquals( m1, merger.merge( m1, null ) );
        Assert.assertEquals( m2, merger.merge( null, m2 ) );
        Assert.assertEquals( map, merger.merge( m2, m1 ) );

    }

    @Test
    public void testArrayMerger() throws Exception {
        
        String[] one1 = new String[] { "1", "2", "3" };
        String[] one2 = new String[] { "4", "5", "6" };
        Object result = ExtensionLoader.getExtensionLoader( Merger.class )
                .getExtension( ArrayMerger.NAME ).merge( one1, one2 );
        Assert.assertTrue( result.getClass().isArray() );
        Assert.assertEquals( 6, Array.getLength( result ) );
        
        String[][] two1 = {
                {"1","1","1",},
                {"1","1","1",},
                {"1","1","1",},
        };
        
        String[][] two2 = {
                {"1","1","1",},
                {"1","1","1",},
                {"1","1","1",},
        };
        
        result = ExtensionLoader.getExtensionLoader( Merger.class )
                .getExtension( ArrayMerger.NAME ).merge( two1, two2 );
        Assert.assertTrue( result.getClass().isArray() );
        Assert.assertEquals( 2, ArrayMerger.getDimensions( result ) );
        Assert.assertEquals( 6, Array.getLength( result ) );
        
        for( int i = 0; i < 6; i++ ) {
            Object item = Array.get( result, i );
            Assert.assertTrue( item.getClass().isArray() );
            Assert.assertEquals( 3, Array.getLength( item ) );
        }

    }

    @Test
    public void testGetDimensions() throws Exception {
        Assert.assertEquals( 3, ArrayMerger.getDimensions( new Object[][][]{
                new Object[][]{
                        new Object[]{  }
                }
        } ) );
        Assert.assertEquals( 1, ArrayMerger.getDimensions( new Object[] {} ) );
        Assert.assertEquals( 0, ArrayMerger.getDimensions( "123" ) );
    }
    
}
