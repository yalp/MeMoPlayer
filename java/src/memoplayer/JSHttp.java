package memoplayer;

import java.lang.ref.WeakReference;

/**
 * Handle JS HTTP calls using the FileQueue
 */
public class JSHttp extends FileQueue {
    private final static String CHUNK_BML = "BML";
    private final static String CHUNK_IMAGE = "IMAGE";

    // Keep a private list of weak refs to http requests
    private static ObjLink s_list;

    static void doHttp (Context c, int m, Register rr[] , int r, int nbP) {
        final Script s = c.script;
        switch (m) {
        case 0: // get (url, callback, [encoding])
            s.releaseMachineOnInit = false;
            new JSHttp(c, rr[r].getString(), rr[r+1].getInt(), nbP != 3 ? "UTF-8" : rr[r+2].getString());
            return;
        case 1: // post (url, data, callback, [encoding])
            s.releaseMachineOnInit = false;
            new JSHttp(c, rr[r].getString(), rr[r+1].getString(), rr[r+2].getInt(), nbP != 4 ? "UTF-8" : rr[r+3].getString());
            return;
        case 2: // cancel (url)
            cancel (rr[r].getString());
            return;
        case 3: // stream (url, chunkCb, statusCb)
            s.releaseMachineOnInit = false;
            new JSHttp(c, rr[r].getString(), rr[r+1].getInt(), rr[r+2].getInt());
            return;
        default:
            System.err.println ("doHttp (m:"+m+")Static call: Invalid method");
            return;
        }
    }

    private static void cancel (String url) {
        ObjLink l = s_list;
        while (l != null) {
            JSHttp h = (JSHttp) ((WeakReference)l.m_object).get();
            if (h != null && url.equals(h.m_url)) {
                h.close(CLOSED);
                s_list = s_list.remove(l.m_object);
                return;
            }
            l = l.m_next;
        }
    }

    private static void add (JSHttp h) {
        WeakReference wr = new WeakReference(h);
        ObjLink l = s_list;
        while (l != null) {
            if (((WeakReference)l.m_object).get() == null) {
                l.m_object = wr;
                return;
            }
            l = l.m_next;
        }
        s_list = ObjLink.create(wr, s_list);
    }

    private final Context m_context;
    private final String m_encoding;
    private final String m_postData;
    private final Script m_script;
    private final int m_statusCb;
    private final Register m_rUrl = new Register();
    private int m_chunkCb = -1;

    /**
     * Stream constructor
     */
    public JSHttp (Context c, String url, int chunkCb, int statusCb) {
        this (c, url, null, statusCb, null);
        m_chunkCb = chunkCb;
    }

    /**
     * Get constructor
     */
    public JSHttp (Context c, String url, int cb, String encoding) {
        this (c, url, null, cb, encoding);
    }

    /**
     * Post constructor
     */
    public JSHttp (Context c, String url, String postData, int cb, String encoding) {
        super (url, c.scene);
        m_context = c;
        m_script = c.script;
        m_url = url;
        m_statusCb = cb;
        m_postData = postData;
        m_encoding = encoding;
        m_rUrl.setString (url);
        FileQueue.add(this);
        add (this);
    }

    public void openAndLoad() {
        if (getState() == QUEUED) {
            if (m_chunkCb == -1) {
                runRequest();
            } else {
                runStream();
            }
        }
    }

    private void runRequest () {
        int response = 0;
        String rdata = "";
        m_mode = m_postData == null ? File.MODE_READ : File.MODE_WRITE;
        m_context.addLoadable (this);
        try {
            open (m_url);
            if (m_mode == File.MODE_WRITE) {
                startWriteAll(m_postData, false, m_encoding);
            }
            rdata = startReadAll (false, m_encoding);
            response = getHttpResponseCode();
        } catch (Throwable t) {
            Logger.println("Error: Http call failed: "+t);
        } finally {
            close(CLOSED);
            m_context.removeLoadable (this);
            Register[] params = new Register[] { m_rUrl, new Register(), new Register() };
            params[1].setInt(response);
            params[2].setString(rdata);
            m_script.addCallback(m_statusCb, params);
            MiniPlayer.wakeUpCanvas();
        }
    }

    public void runStream () {
        try {
            //Logger.println("START REQ: "+m_url);
            open (m_url);
            int code = getHttpResponseCode();
            //Logger.println("REQ: "+m_url+" CODE:"+code);
            if (code == 200) {
                notifyStatus (code, "streaming");
                try {
                    while (decodeChunk());
                    notifyStatus (222, "done");
                } finally {
                    close (LOADED);
                }
            } else {
                notifyStatus (code, "error: bad response code");
            }
        } catch (Throwable t) {
            notifyStatus (0, "error: "+t+": "+t.getMessage());
        }
    }

    boolean decodeChunk () throws Exception {
        int magic = readInt ();
        //Logger.println("CHUNK: "+Integer.toHexString(magic));
        switch (magic) {
        case Decoder.MAGIC_IMAGE:
            String name = readString ();
            int size = readInt ();
            //Logger.println("IMAGE: "+name+": "+size);
            byte[] data = readBytes(size);
            m_scene.addData (name, data, magic, true);
            notifyChunk(CHUNK_IMAGE, name, -1);
            return true;
//#ifdef api.bml
        case Decoder.MAGIC_BML:
            name = readString ();
            size = readInt ();
            //Logger.println("BML: "+name+": "+size);
            data = readBytes (size);
            XmlNode root = new BmlReader(data).getRootNode();
            int id = ExternCall.getFreeXmlSlot();
            if (id == -1) throw new Exception ("No Xml slot available !");
            XmlDom dom = new XmlDom ();
            dom.setRoot(root);
            ExternCall.s_xmlDoms[id] = new XmlDom ();
            notifyChunk (CHUNK_BML, name, id);
            return true;
//#endif
        case Decoder.MAGIC_END:
            return false;
        default:
            // Skip unsupported chunk
            name = readString ();
            size = readInt ();
            readBytes(size);
            Logger.println("Stream: Skipped unsupported chunk: "+Integer.toHexString(magic));
            return true;
            //throw new Exception("Bad magic: "+ Integer.toHexString(magic));
        }
    }

    void notifyStatus (int status, String message) {
        final Register rStatus = new Register();
        final Register rMessage = new Register();
        rStatus.setInt (status);
        rMessage.setString (message);
        m_script.addCallback (m_statusCb, new Register[] { m_rUrl, rStatus, rMessage });
        MiniPlayer.wakeUpCanvas();
        Thread.yield();
    }

    void notifyChunk (String type, String name, int id) {
        final Register regName = new Register();
        final Register regType = new Register();
        final Register regId = new Register();
        regName.setString (name);
        regType.setString (type);
        regId.setInt(id);
        m_script.addCallback (m_chunkCb, new Register[] { regName, regType, regId });
        MiniPlayer.wakeUpCanvas();
        Thread.yield();
    }
}
