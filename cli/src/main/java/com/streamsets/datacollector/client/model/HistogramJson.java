/**
 * Copyright 2015 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.datacollector.client.model;

import com.streamsets.datacollector.client.StringUtil;



import io.swagger.annotations.*;
import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaClientCodegen", date = "2015-09-11T14:51:29.367-07:00")
public class HistogramJson   {

  private Long count = null;
  private Long max = null;
  private Long mean = null;
  private Long min = null;
  private Long p50 = null;
  private Long p75 = null;
  private Long p95 = null;
  private Long p98 = null;
  private Long p99 = null;
  private Long p999 = null;
  private Long stddev = null;


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("count")
  public Long getCount() {
    return count;
  }
  public void setCount(Long count) {
    this.count = count;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("max")
  public Long getMax() {
    return max;
  }
  public void setMax(Long max) {
    this.max = max;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("mean")
  public Long getMean() {
    return mean;
  }
  public void setMean(Long mean) {
    this.mean = mean;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("min")
  public Long getMin() {
    return min;
  }
  public void setMin(Long min) {
    this.min = min;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("p50")
  public Long getP50() {
    return p50;
  }
  public void setP50(Long p50) {
    this.p50 = p50;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("p75")
  public Long getP75() {
    return p75;
  }
  public void setP75(Long p75) {
    this.p75 = p75;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("p95")
  public Long getP95() {
    return p95;
  }
  public void setP95(Long p95) {
    this.p95 = p95;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("p98")
  public Long getP98() {
    return p98;
  }
  public void setP98(Long p98) {
    this.p98 = p98;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("p99")
  public Long getP99() {
    return p99;
  }
  public void setP99(Long p99) {
    this.p99 = p99;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("p999")
  public Long getP999() {
    return p999;
  }
  public void setP999(Long p999) {
    this.p999 = p999;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("stddev")
  public Long getStddev() {
    return stddev;
  }
  public void setStddev(Long stddev) {
    this.stddev = stddev;
  }



  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class HistogramJson {\n");

    sb.append("    count: ").append(StringUtil.toIndentedString(count)).append("\n");
    sb.append("    max: ").append(StringUtil.toIndentedString(max)).append("\n");
    sb.append("    mean: ").append(StringUtil.toIndentedString(mean)).append("\n");
    sb.append("    min: ").append(StringUtil.toIndentedString(min)).append("\n");
    sb.append("    p50: ").append(StringUtil.toIndentedString(p50)).append("\n");
    sb.append("    p75: ").append(StringUtil.toIndentedString(p75)).append("\n");
    sb.append("    p95: ").append(StringUtil.toIndentedString(p95)).append("\n");
    sb.append("    p98: ").append(StringUtil.toIndentedString(p98)).append("\n");
    sb.append("    p99: ").append(StringUtil.toIndentedString(p99)).append("\n");
    sb.append("    p999: ").append(StringUtil.toIndentedString(p999)).append("\n");
    sb.append("    stddev: ").append(StringUtil.toIndentedString(stddev)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
