/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.ohmage.prompt.media;

import org.ohmage.db.Models.Response;
import org.ohmage.prompt.AbstractPromptFragment;

import java.io.File;
import java.util.UUID;

public abstract class MediaPromptFragment extends AbstractPromptFragment {

    String uuid;

    public MediaPromptFragment() {
        super();
    }

    /**
     * Deletes the image from the file system if it was taken
     */
    public void delete() {
        if (isPromptAnswered()) {
            getMedia().delete();
        }
    }

    @Override
    protected void clearTypeSpecificResponseData() {
        // Delete the old file
        delete();
        uuid = null;
    }

    /**
     * Returns true if the UUID is not null meaning that we have at least some
     * image that we are referencing.
     */
    @Override
    public boolean isPromptAnswered() {
        return (uuid != null) && getMedia().exists();
    }

    @Override
    protected Object getTypeSpecificResponseObject() {
        return uuid;
    }

    @Override
    protected Object getTypeSpecificExtrasObject() {
        return null;
    }

    protected File getMedia() {
        if (uuid == null)
            uuid = UUID.randomUUID().toString();
        return Response.getTemporaryResponsesMedia(uuid);
    }
}
