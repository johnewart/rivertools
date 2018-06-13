package net.johnewart.rivertools.core;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 12/29/12
 * Time: 9:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class AerialMap extends ImageMap {

    @JsonProperty
    public Long id;

    @JsonProperty
    public boolean job_complete;

    @JsonProperty
    public String job_handle;

    @JsonProperty
    public String resource_uri;

    @JsonProperty
    public Long height;

    @JsonProperty
    public Long width;
}
