/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.event.retrievestatus;

import java.util.List;
import java.util.Map;

import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.download.ReliableResourceDownloader;

/**
 * This class is a backend information gathering and aggregation class. It holds information about all downloads and then returns this
 * information when requested, usually by {@link org.codice.ddf.catalog.admin.downloadmanager.DownloadManagerService}.
 * Most of the information is accessed through a reference to the {@link ddf.catalog.resource.download.ReliableResourceDownloadManager}
 * responsible for the download.
 */

public interface DownloadStatusContainer extends DownloadController {

    /**
     * Adds a {@link ddf.catalog.resource.download.ReliableResourceDownloadManager} to the Map, which is used by
     * {@link this.getDownloadStatus}. Currently this is called in {@link ddf.catalog.resource.download.ReliableResourceDownloadManager}
     *
     * @param downloadIdentifier Randomly generated String assigned to download at its start.
     * @param downloadManager    The Object that handles the download; {@link this} uses it to gather information about
     *                           the download.
     */
    void addDownloadInfo(String downloadIdentifier, ReliableResourceDownloader downloader,
            ResourceResponse resourceResponse);

    /**
     * Function to remove the map entry corresponding to the downloadIdentifer passed it. This means it will no longer be
     * returned by {@link this.getAllDownloadsStatus}, {@link this.getDownloadStatus}, or {@link this.getAllDownloads}.
     *
     * @param downloadIdentifier The randomly generated downloadId string assigned to the download at its start.
     */
    void removeDownloadInfo(String downloadIdentifier);
}
