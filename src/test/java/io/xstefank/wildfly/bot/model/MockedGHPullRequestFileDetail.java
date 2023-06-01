package io.xstefank.wildfly.bot.model;

import org.kohsuke.github.GHPullRequestFileDetail;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Unfortunately, since {@link GHPullRequestFileDetail} has package-private fields with no setters, we need to create
 * this mock to set these fields.
 */
public class MockedGHPullRequestFileDetail extends GHPullRequestFileDetail {

    String sha;
    String filename;
    String status;
    int additions;
    int deletions;
    int changes;
    String blob_url;
    String raw_url;
    String contents_url;
    String patch;
    String previous_filename;

    public MockedGHPullRequestFileDetail(String sha, String filename, String status, int additions, int deletions,
                                         int changes, String blob_url, String raw_url, String contents_url, String patch,
                                         String previous_filename) {
        this.sha = sha;
        this.filename = filename;
        this.status = status;
        this.additions = additions;
        this.deletions = deletions;
        this.changes = changes;
        this.blob_url = blob_url;
        this.raw_url = raw_url;
        this.contents_url = contents_url;
        this.patch = patch;
        this.previous_filename = previous_filename;
    }

    @Override
    public String getSha() {
        return sha;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public int getAdditions() {
        return additions;
    }

    @Override
    public int getDeletions() {
        return deletions;
    }

    @Override
    public int getChanges() {
        return changes;
    }

    @Override
    public URL getBlobUrl() {
        try {
            return URI.create(blob_url).toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public URL getRawUrl() {
        try {
            return URI.create(raw_url).toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public URL getContentsUrl() {
        try {
            return URI.create(contents_url).toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public String getPatch() {
        return patch;
    }

    @Override
    public String getPreviousFilename() {
        return previous_filename;
    }
}
