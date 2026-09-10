import pandas as pd
import logging
from datetime import datetime
from azure.data.tables import TableClient
from azure.core.exceptions import HttpResponseError

# Configurazione del logging per il nostro script
logging.basicConfig(level=logging.INFO, format='%(levelname)s: %(message)s')

# Silenziare i log di rete interni di Azure
logging.getLogger('azure').setLevel(logging.WARNING)
logging.getLogger('urllib3').setLevel(logging.WARNING)

# --- CONFIGURAZIONI AZURE ---
CONNECTION_STRING = "secret"
TABLE_NAME = "pagopadweuafmsaissuerrangetable"
EXCEL_FILE_PATH = "input_file.xlsx"

def backup_table_to_csv(table_client):
    """
    Scarica tutte le entità dalla tabella Azure e le salva in un file CSV in locale.
    """
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_file_path = f"backup_{TABLE_NAME}_{timestamp}.csv"

    logging.info("Inizio creazione del backup locale della tabella...")

    try:
        # list_entities() recupera tutti i record (gestisce la paginazione in automatico)
        entities = list(table_client.list_entities())

        if not entities:
            logging.info("La tabella è vuota. Nessun dato esportato nel backup.")
            return True

        # Convertiamo la lista di dizionari in un DataFrame Pandas
        df_backup = pd.DataFrame(entities)

        # Salviamo in CSV
        df_backup.to_csv(backup_file_path, index=False)
        logging.info(f"Backup completato con successo. Dati salvati in: {backup_file_path} (Totale righe: {len(entities)})")
        return True

    except Exception as e:
        logging.error(f"Errore critico durante il backup della tabella: {e}")
        return False

def process_and_insert_data():
    # 1. Inizializza il TableClient
    try:
        table_client = TableClient.from_connection_string(conn_str=CONNECTION_STRING, table_name=TABLE_NAME)
    except Exception as e:
        logging.error(f"Errore durante la connessione ad Azure Table Storage: {e}")
        return

    # 1.5 Esegui il Backup prima di fare qualsiasi modifica
    backup_success = backup_table_to_csv(table_client)
    if not backup_success:
        logging.error("Interruzione del processo: Impossibile procedere senza un backup valido.")
        return # Blocchiamo l'esecuzione se il backup fallisce

    # 2. Leggi il file Excel
    try:
        df = pd.read_excel(EXCEL_FILE_PATH, dtype=str) # Leggiamo tutto come stringa
        df.fillna('', inplace=True) # Sostituiamo i NaN con stringhe vuote
    except Exception as e:
        logging.error(f"Errore durante la lettura del file Excel: {e}")
        return

    # 3. Itera su ogni riga del DataFrame
    for index, row in df.iterrows():
        original_bin = str(row['BIN']).strip()

        # Ignora le righe vuote
        if not original_bin:
            continue

        # - prendi il bin e aggiungi tanti 0 fino ad arrivare a 19 caratteri
        padded_bin = original_bin.ljust(19, '0')

        # - esegui una query sullo storage e verifica che il bin con padding non sia compreso in nessun range
        query_filter = f"LOW_RANGE le '{padded_bin}' and HIGH_RANGE ge '{padded_bin}'"

        try:
            # Eseguiamo la query limitando il risultato a 1 per efficienza (basta trovare una corrispondenza)
            results = list(table_client.query_entities(query_filter=query_filter))

            if len(results) > 0:
                # Il risultato della query non è vuoto -> skip e logga un warning
                logging.warning(f"Row {index+2} - BIN {original_bin} (padded: {padded_bin}) saltato: è già compreso in un range esistente nella tabella.")
                continue

        except HttpResponseError as e:
            logging.error(f"Errore durante la query per il BIN {original_bin}: {e.message}")
            continue

        # - se è vuoto allora procedi ad inserire la riga

        # Preparazione dei campi formattati
        high_range = str(row['HIGH_RANGE']).strip().ljust(19, '9')
        low_range = str(row['LOW_RANGE']).strip().ljust(19, '0')

        # Estrarre solo il carattere prima del trattino
        product_category_raw = str(row['PRODUCT_CATEGORY']).strip()
        product_category = product_category_raw.split('-')[0].strip() if '-' in product_category_raw else product_category_raw

        # Creazione dell'entità
        entity = {
            "PartitionKey": original_bin,
            "RowKey": original_bin,
            "ISSUER_ID": str(row['ABI']).strip(),
            "ABI": str(row['ABI']).strip(),
            "CIRCUIT": str(row['CIRCUIT']).strip(),
            "HIGH_RANGE": high_range,
            "LOW_RANGE": low_range,
            "PRODUCT_CATEGORY": product_category
        }

        # Inserimento nella Table
        try:
            table_client.create_entity(entity=entity)
            logging.info(f"Row {index+2} - BIN {original_bin} inserito con successo.")
        except HttpResponseError as e:
            if e.status_code == 409:
                logging.warning(f"Row {index+2} - L'entità per il BIN {original_bin} esiste già.")
            else:
                logging.error(f"Errore durante l'inserimento per il BIN {original_bin}: {e.message}")

if __name__ == "__main__":
    process_and_insert_data()