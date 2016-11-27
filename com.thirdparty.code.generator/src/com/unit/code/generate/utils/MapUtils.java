package com.unit.code.generate.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class MapUtils {

	public static List<Map.Entry<String, Integer>> sortMapByValue( Map<String, Integer> map ) {
		List<Map.Entry<String, Integer>> mapList = new ArrayList<Map.Entry<String, Integer>>(
				map.entrySet() );
		Collections.sort( mapList, new Comparator<Map.Entry<String, Integer>>() {

			@Override
			public int compare( Entry<String, Integer> o1,
					Entry<String, Integer> o2 ) {
				return o1.getValue() - o2.getValue();
			}
		} );
		
		return mapList;
	}
}
