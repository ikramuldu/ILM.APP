/*
 * Copyright 2016 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bn.poro.quran.activity_reader.pdfviewer.util;

public @interface Constants {
    boolean DEBUG_MODE = false;
    /**
     * Between 0 and 1, the thumbnails quality (default 0.3). Increasing this value may cause performance decrease
     */
    float THUMBNAIL_RATIO = 0.3f;
    /**
     * The size of the rendered parts (default 256)
     * Tinier : a little bit slower to have the whole page rendered but more reactive.
     * Bigger : user will have to wait longer to have the first visual results
     */
    float PART_SIZE = 400;
    /**
     * Part of document above and below screen that should be preloaded, in dp
     */
    int PRELOAD_OFFSET = 20;
    float MAXIMUM_ZOOM = 10;
    float MINIMUM_ZOOM = 1;
    /**
     * The size of the cache (number of bitmaps kept)
     */
    int CACHE_SIZE = 120;
    int THUMBNAILS_CACHE_SIZE = 8;
}
