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

import java.io.Serializable;
import java.util.List;

class DataSegment<ITEM> extends Segment<ITEM> implements Serializable {

    private List<ITEM> data;

    DataSegment(PagerDataModel<ITEM> model, int page) {
        super(model, page);
    }

    ITEM get(int position) {
        if (data == null) return null;
        int index = position - getFrom();
        if (index < data.size()) return data.get(index);
        return null;
    }

    void setData(List<ITEM> items) {
        this.data = items;
        model.onPageAvailable(this);
    }
}
