package com.cyberIyke.allvideodowloader.interfaces;

import com.cyberIyke.allvideodowloader.helper.DownloadVideo;

//interface created outside DownloadsInactive in a different file to avoid cyclic inheritance
public interface OnDownloadWithNewLinkListener {
    void onDownloadWithNewLink(DownloadVideo download);
}
