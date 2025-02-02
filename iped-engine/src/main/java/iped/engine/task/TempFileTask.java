package iped.engine.task;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import iped.configuration.Configurable;
import iped.data.IItem;
import iped.engine.CmdLineArgs;
import iped.engine.config.ConfigurationManager;
import iped.engine.config.LocalConfig;
import iped.engine.data.Item;
import iped.utils.IOUtil;

/**
 * Tarefa para geração de arquivos temporários para os itens antes do
 * processamento. Caso indexTemp esteja em disco SSD e a imagem esteja
 * compactada (e01), pode aumentar consideravelmente a performance pois os itens
 * deixam de ser descompactados múltiplas vezes pela libewf, a qual não é thread
 * safe e sincroniza descompactações concorrentes, subaproveitando máquinas
 * multiprocessadas.
 *
 * @author Nassif
 *
 */
public class TempFileTask extends AbstractTask {

    private static Logger LOGGER = LoggerFactory.getLogger(TempFileTask.class);
    private static int MAX_TEMPFILE_LEN = 1024 * 1024 * 1024;
    private boolean indexTempOnSSD = false;
    private boolean isEnabled = true;

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public List<Configurable<?>> getConfigurables() {
        return Collections.emptyList();
    }

    @Override
    public void init(ConfigurationManager configurationManager) throws Exception {
        LocalConfig config = configurationManager.findObject(LocalConfig.class);
        indexTempOnSSD = config.isIndexTempOnSSD();
        CmdLineArgs args = (CmdLineArgs) caseData.getCaseObject(CmdLineArgs.class.getName());
        isEnabled = !"fastmode".equals(args.getProfile()) && !"triage".equals(args.getProfile());
    }

    @Override
    public void finish() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    protected void process(IItem evidence) throws Exception {


        if (evidence instanceof Item) {
            if (((Item) evidence).cacheDataInMemory()) {
                return;
            }
        }

        if (!indexTempOnSSD) {
            return;
        }

        Long len = evidence.getLength();
        if (len != null && len <= MAX_TEMPFILE_LEN) {
            try {
                // skip items with File refs && carved items pointing to parent temp file
                if (!IOUtil.hasFile(evidence) && (!(evidence instanceof Item) || !((Item) evidence).hasParentTmpFile())) {
                    if (!evidence.isSubItem()) { // should we create temp files for subitems compressed into the sqlite storages?
                        evidence.getTempFile();
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("{} Error creating temp file {} {}", Thread.currentThread().getName(), evidence.getPath(), //$NON-NLS-1$
                        e.toString());
            }
        }

    }

}
