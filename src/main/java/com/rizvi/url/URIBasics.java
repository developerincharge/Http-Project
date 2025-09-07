package com.rizvi.url;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class URIBasics {

    public static void main(String[] args)  {
       URI baseSite = URI.create("https://learningprogramming.academy");
        URI timSite = URI.create("https://learningprogramming.academy/courses/complete-java-masterclass");
        print(timSite);

        try{
        URI uri = new URI("http://user:pw@store.com:5000/products/phones?os=android#samsung");
        print(uri);
           URI masterClass = baseSite.resolve(timSite);
           print(masterClass);
           URI masterClass2 = baseSite.resolve("courses/complete-java-masterclass?instructor=tim&duration=100#discount");
           print(masterClass2);
           URI masterClass3 = baseSite.resolve("courses/complete-java-masterclass-3?course=complete-java-masterclass-3&instructor=tim&duration=100#discount");
           print(masterClass3);

            URL url = masterClass3.toURL();
            System.out.println(url);
            print(url);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }


    }

    private static void print(URI uri){
        System.out.printf("""
                _________________________________________
                [schema:]schema-specific-part[#fragment]
                -----------------------------------------
                Scheme: %s
                Schema-specific-part: %s
                   Authority: %s
                      User info: %s
                      Host: %s
                      Port: %s
                      Path: %s
                      Query: %s
                Fragment: %s
                """,
                uri.getScheme(),
                uri.getSchemeSpecificPart(),
                uri.getAuthority(),
                uri.getUserInfo(),
                uri.getHost(),
                uri.getPort(),
                uri.getPath(),
                uri.getQuery(),
                uri.getFragment()
        );
    }

    private static void print(URL url){
        System.out.printf("""
                   Authority: %s
                     User info: %s
                     Host: %s
                     Port: %s
                     Path: %s
                     Query: %s
                   """,
                url.getAuthority(),
                url.getUserInfo(),
                url.getHost(),
                url.getPort(),
                url.getPath(),
                url.getQuery()
        );
    }
}
