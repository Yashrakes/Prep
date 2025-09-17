package com.HLDLLD.corejava.streams;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class collectors {
    public static void main(String[] args) {

        List<productdem> productdemList = Arrays.asList(
                new productdem("apple", 1000),
                new productdem("samsung", 1200),
                new productdem("nokia", 1400),
                new productdem("MI", 1400)
        );
// collectingand then
        String maxpriceproduct = productdemList.stream().collect(Collectors.collectingAndThen(
                Collectors.maxBy(Comparator.comparing(productdem::getPrice)),
                (poptional -> poptional.isPresent() ?poptional.get().getName():"NONE")
        ));

        System.out.println(maxpriceproduct);

//grouping by
        Map<Integer, List<productdem>> mapbyprice = productdemList.stream().collect(Collectors.groupingBy(
                productdem ::getPrice
        ));
        System.out.println(mapbyprice);

//partition by

        Map<Boolean, List<productdem>> parti = productdemList.stream().collect(Collectors.partitioningBy(
                produ-> produ.getPrice()>1200));
        System.out.println(parti);

    }

//
}
