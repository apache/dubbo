/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.model.media;

import java.util.List;

@SuppressWarnings("serial")
public class Media implements java.io.Serializable {
    public String uri;
    public String title;        // Can be unset.
    public int width;
    public int height;
    public String format;
    public long duration;
    public long size;
    public int bitrate;         // Can be unset.
    public boolean hasBitrate;
    public List<String> persons;
    public Player player;
    public String copyright;    // Can be unset.

    public Media() {
    }

    public Media(String uri, String title, int width, int height, String format, long duration, long size, int bitrate, boolean hasBitrate, List<String> persons, Player player, String copyright) {
        this.uri = uri;
        this.title = title;
        this.width = width;
        this.height = height;
        this.format = format;
        this.duration = duration;
        this.size = size;
        this.bitrate = bitrate;
        this.hasBitrate = hasBitrate;
        this.persons = persons;
        this.player = player;
        this.copyright = copyright;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Media media = (Media) o;

        if (bitrate != media.bitrate) return false;
        if (duration != media.duration) return false;
        if (hasBitrate != media.hasBitrate) return false;
        if (height != media.height) return false;
        if (size != media.size) return false;
        if (width != media.width) return false;
        if (copyright != null ? !copyright.equals(media.copyright) : media.copyright != null) return false;
        if (format != null ? !format.equals(media.format) : media.format != null) return false;
        if (persons != null ? !persons.equals(media.persons) : media.persons != null) return false;
        if (player != media.player) return false;
        if (title != null ? !title.equals(media.title) : media.title != null) return false;
        if (uri != null ? !uri.equals(media.uri) : media.uri != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + (format != null ? format.hashCode() : 0);
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + bitrate;
        result = 31 * result + (hasBitrate ? 1 : 0);
        result = 31 * result + (persons != null ? persons.hashCode() : 0);
        result = 31 * result + (player != null ? player.hashCode() : 0);
        result = 31 * result + (copyright != null ? copyright.hashCode() : 0);
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Media ");
        sb.append("uri=").append(uri);
        sb.append(", title=").append(title);
        sb.append(", width=").append(width);
        sb.append(", height=").append(height);
        sb.append(", format=").append(format);
        sb.append(", duration=").append(duration);
        sb.append(", size=").append(size);
        sb.append(", hasBitrate=").append(hasBitrate);
        sb.append(", bitrate=").append(String.valueOf(bitrate));
        sb.append(", persons=").append(persons);
        sb.append(", player=").append(player);
        sb.append(", copyright=").append(copyright);
        sb.append("]");
        return sb.toString();
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
        this.hasBitrate = true;
    }

    public List<String> getPersons() {
        return persons;
    }

    public void setPersons(List<String> persons) {
        this.persons = persons;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public enum Player {
        JAVA, FLASH
    }
}