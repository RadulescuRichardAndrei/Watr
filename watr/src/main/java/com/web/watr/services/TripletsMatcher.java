package com.web.watr.services;

import com.web.watr.services.query.GenericQueryService;
import com.web.watr.utils.MethodUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

public class TripletsMatcher {

    private GenericQueryService queryService;
    public static List<AbstractMap.SimpleEntry<String,String>> stringMatching(List<String> firstList, List<String> secondList, int maxDistance){
        var matchingPairs = new ArrayList<AbstractMap.SimpleEntry<String,String>>();

        // Create a LevenshteinDistance instance with the maxDistance
        LevenshteinDistance levenshtein = new LevenshteinDistance(10);

        for (String first : firstList) {
            for (String second : secondList) {
                // Calculate Levenshtein distance
                Integer distance = levenshtein.apply(MethodUtils.extractLast(first), MethodUtils.extractLast(second));

                // Check if the distance is within the threshold
                if (distance != null && distance <= maxDistance) {
                    // Add the pair to the result
                    matchingPairs.add(new AbstractMap.SimpleEntry<>(first, second) {
                    });
                }
            }
        }

        return matchingPairs;
    }

}
