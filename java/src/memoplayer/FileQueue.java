/*
 * Copyright (C) 2010 France Telecom
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

package memoplayer;

/**
 * Interface for a class requesting a FileQueue request.
 */
interface FileRequester {
    /**
     * Called from the FileQueue thread on file content availability
     * @param data the content of the File or null on error
     */
    void dataReady (byte[] data);
}


/**
 * Prevent multiple concurrent async files access to HTTP resources.
 * A single thread will consume the queue and notify the requesters.
 */
public class FileQueue extends File {

    private static Thread[] s_threads;
    private static boolean s_quit;
    private static FileQueue s_queue;
    private static int s_maxNbThreads = 2;
    static {
        try {
            s_maxNbThreads = Integer.parseInt(MiniPlayer.getJadProperty("MeMo-DLThreads"));
        } catch (Exception e) {
        }
    }

    /**
     * Request a file, requester will be notified on completion or error.
     * The returned FileQueue can be shared across several requesters.
     */
    public static synchronized FileQueue add (String url, Scene s, FileRequester fr, boolean isImage) {
        if (s.isBlacklisted (url)) { // already known as a incorrect url
            fr.dataReady (null);
            return null;
        }
        if (s_queue == null) {
            s_queue = new FileQueue(url, s, fr, isImage);
            startThreads();
            return s_queue;
        }
        return s_queue.findOrAdd (url, s, fr, isImage);
    }

    /**
     * Add a unique FileQueue to the top of the queue.
     */
    public static synchronized void add (FileQueue f) {
        f.m_next = s_queue;
        s_queue = f;
        startThreads();
    }

    /**
     * Cancel all file requests linked to a given scene
     */
    public static synchronized void cancelAll (Scene s) {
        if (s_queue != null) {
            s_queue = s_queue.cancel (s);
        }
    }

    /**
     * Must be called by MyCanvas on application exit
     */
    public static void stopThread () {
        if (s_threads != null) {
            s_queue = null;
            synchronized (s_threads) {
                s_quit = true;
                s_threads.notifyAll();
            }
        }
    }

    /**
     * Start or wakeup the FileQueue thread to consume the queue
     */
    private static void startThreads () {
        if (s_threads == null) {
            s_threads = new Thread[s_maxNbThreads];
            s_quit = false;
            for (int i=0; i<s_maxNbThreads; i++) {
                s_threads[i] = new Thread() {
                    public void run() {
                        while (true) {
                            try {
                                FileQueue f = pop();
                                while (f != null) {
//#ifdef MM.namespace
                                    // Force the namespace associated to this thread for File access
                                    forceNamespace (f.m_cacheNamespace);
//#endif
                                    f.openAndLoad();
                                    f = pop();
                                }
                                synchronized (s_threads) {
                                    if (s_quit) {
                                        break;
                                    }
                                    try { s_threads.wait(); } catch (InterruptedException e) {};
                                }
                            } catch (Throwable e) {
                                Logger.println("Exception in FileQueue: " + e + ":" + e.getMessage());
                            }
                        }
                    }
                };
                s_threads[i].start();
            }
        } else {
            // Wake up
            synchronized (s_threads) {
                s_threads.notify();
            }
        }
    }

    private static synchronized FileQueue pop () {
        FileQueue head = s_queue;
        if (head != null) {
            head = head.popQueue();
            if (head != null) {
                s_queue = head.m_next;
                return head;
            }
            s_queue = null;
        }
        return null;
    }

    private FileQueue m_next;
    private ObjLink m_requesters;
    protected Scene m_scene;
    private boolean m_isImage;

    protected FileQueue (String url, Scene s) {
        m_url = url;
        m_scene = s;
//#ifdef MM.namespace
        m_cacheNamespace = Thread.currentNamespace();
//#endif
        setState(QUEUED);
    }

    private FileQueue (String url, Scene s, FileRequester fr, boolean isImage) {
        this(url, s);
        m_requesters = ObjLink.create(fr, null);
        m_isImage = isImage;
    }
    
    /**
     * Called by the requester to cancel a request.
     * The request will only be cancel if no other requesters is still waiting.
     * Called from main thread.
     */
    public synchronized void cancel (FileRequester fr) {
        if (m_requesters != null) {
            m_requesters = m_requesters.remove(fr);
            if (m_requesters == null) {
                m_scene = null;
            }
        }
    }

    /**
     * Called by the FileQueue thread to notify requesters.
     * @param data File content or null on error
     */
    private synchronized void notifyRequesters (byte[] data) {
        ObjLink ol = m_requesters;
        while (ol != null) {
            ((FileRequester)ol.m_object).dataReady(data);
            ol = ol.m_next;
        }
        ObjLink.releaseAll(m_requesters);
        m_requesters = null;
    }

    /**
     * Find a similar request or add one at the end of the queue.
     * Called from main thread.
     */
    private FileQueue findOrAdd (String url, Scene s, FileRequester fr, boolean isImage) {
        if (url.equals(m_url) && (m_scene == null || m_scene == s)) {
            m_scene = s;
            m_requesters = ObjLink.create(fr, m_requesters);
            return this;
        } else if (m_next != null) {
            return m_next.findOrAdd (url, s, fr, isImage);
        }
        return m_next = new FileQueue (url, s, fr, isImage);
    }

    /**
     * Cancel all FileQueue queued for a given scene.
     * Called from main thread.
     */
    private FileQueue cancel (Scene scene) {
        if (m_scene == scene) {
            m_scene = null;
            ObjLink.releaseAll(m_requesters);
            m_requesters = null;
        }
        if (m_next != null) {
            return m_next.cancel(scene);
        }
        return this;
    }

    /**
     * Find next File to use from queue recursively.
     * Called from FileQueue thread.
     */
    private FileQueue popQueue () {
        if (m_scene == null) {
            if (m_next != null) {
                return m_next.popQueue();
            }
            return null;
        }
        return this;
    }

    /**
     * Open queued file, load data to m_data, close file.
     * Called from FileQueue thread.
     */
    protected void openAndLoad() {
        if (getState() == QUEUED) {
            open (m_url);
            if (getState() == READY) {
                setState(LOADING);
                byte[] data = readAllBytes();
                Scene s = m_scene;
                if (data != null) {
                    setState(LOADED);
                    if (m_isImage && s != null) {
                        s.addData(m_url, data, Decoder.MAGIC_IMAGE, true);
                    }
                    notifyRequesters (data);
                } else {
                    if (s != null) {
                        s.addBlacklist(m_url);
                    }
                    notifyRequesters (null);
                }
                close(CLOSED);
                MiniPlayer.wakeUpCanvas();
            }
        }
    }
}
