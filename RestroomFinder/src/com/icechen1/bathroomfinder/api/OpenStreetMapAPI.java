package com.icechen1.bathroomfinder.api;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

public class OpenStreetMapAPI extends DefaultApi10a
{
  private static final String AUTHORIZATION_URL = "http://www.openstreetmap.org/oauth/authorize?oauth_token=%s";
  
  @Override
  public String getAccessTokenEndpoint()
  {
    return "http://www.openstreetmap.org/oauth/access_token";
  }

  @Override
  public String getRequestTokenEndpoint()
  {
    return "http://www.openstreetmap.org/oauth/request_token";
  }
  
  @Override
  public String getAuthorizationUrl(Token requestToken)
  {
    return String.format(AUTHORIZATION_URL, requestToken.getToken());
  }
}