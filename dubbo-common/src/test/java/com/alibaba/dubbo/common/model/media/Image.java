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
package com.alibaba.dubbo.common.model.media;


public class Image implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public String uri;
    public String title;  // Can be null
    public int width;
    public int height;
    public Size size;

    public Image() {
    }

    public Image(String uri, String title, int width, int height, Size size) {
        this.height = height;
        this.title = title;
        this.uri = uri;
        this.width = width;
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Image image = (Image) o;

        if (height != image.height) return false;
        if (width != image.width) return false;
        if (size != image.size) return false;
        if (title != null ? !title.equals(image.title) : image.title != null) return false;
        if (uri != null ? !uri.equals(image.uri) : image.uri != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + (size != null ? size.hashCode() : 0);
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Image ");
        sb.append("uri=").append(uri);
        sb.append(", title=").append(title);
        sb.append(", width=").append(width);
        sb.append(", height=").append(height);
        sb.append(", size=").append(size);
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

    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    public enum Size {
        SMALL, LARGE
    }
}