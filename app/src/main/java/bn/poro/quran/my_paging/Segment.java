/*
Copyright 2017 Audrius Meskauskas

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package bn.poro.quran.my_paging;

import androidx.annotation.NonNull;

class Segment<ITEM> {
    protected final transient PagerDataModel<ITEM> model;
    private final int pageNumber;

    Segment(PagerDataModel<ITEM> model, int page) {
        this.model = model;
        this.pageNumber = page;
    }

    int getFrom() {
        return PagerDataModel.PAGE_SIZE * pageNumber;
    }

    int getTo() {
        return PagerDataModel.PAGE_SIZE * (pageNumber + 1);
    }

    int getPage() {
        return pageNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Segment<?> segment = (Segment<?>) o;
        return pageNumber == segment.pageNumber;
    }

    @Override
    public int hashCode() {
        return pageNumber;
    }

    @NonNull
    public String toString() {
        return "page " + pageNumber;
    }
}
