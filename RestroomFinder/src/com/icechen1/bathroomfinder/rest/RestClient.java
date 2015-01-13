
package com.icechen1.bathroomfinder.rest;

import com.googlecode.androidannotations.annotations.rest.Get;
import com.googlecode.androidannotations.annotations.rest.Rest;

@Rest("http://10.0.0.2")
public interface RestClient {


    @Get("/")
    public abstract void main();

}
