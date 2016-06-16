package ddf.catalog.event.retrievestatus;

import java.util.List;
import java.util.Map;

/**
 * Created by lambeaux on 6/16/16.
 */
public interface DownloadController {

    /**
     * Function to get all downloads.
     *
     * @return Returns an array of downloadIdentifier Strings
     */
    List<String> getAllDownloads();

    /**
     * Function to get all downloads for a specific user.
     *
     * @param userId The id of the user.
     * @return Returns an array of downloadIdentifier Strings, similar to {@link this.getAllDownloads}.
     */
    List<String> getAllDownloads(String userId);

    /**
     * Function to get information about a specific download.
     *
     * @param downloadIdentifier The randomly generated downloadId string assigned to the download at its start.
     * @return Returns a map of attributes describing the download; see {@link this.getAllDownloadsStatus} for details.
     */
    Map<String, String> getDownloadStatus(String downloadIdentifier);

    /**
     * Function for admin to cancel a download. Throws a "cancel" event.
     *
     * @param userId             The Id assigned to the user who is downloading.
     * @param downloadIdentifier The randomly generated downloadId string assigned to the download at its start.
     */
    void cancelDownload(String userId, String downloadIdentifier);

    /**
     * Pause a download in progress.
     *
     * @param userId
     * @param downloadIdentifier
     */
    void pauseDownload(String userId, String downloadIdentifier);
}
