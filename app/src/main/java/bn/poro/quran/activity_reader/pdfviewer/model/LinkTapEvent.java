/*
 * Copyright 2016 Bartosz Schiller
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bn.poro.quran.activity_reader.pdfviewer.model;

import android.graphics.RectF;

import com.shockwave.pdfium.PdfDocument;

public class LinkTapEvent {
    private final PdfDocument.Link link;

    public LinkTapEvent(float originalX, float originalY, float documentX, float documentY, RectF mappedLinkRect, PdfDocument.Link link) {
        this.link = link;
    }

    public PdfDocument.Link getLink() {
        return link;
    }
}
