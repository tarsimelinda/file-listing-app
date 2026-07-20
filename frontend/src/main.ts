import './style.css';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

type FileListResponse = {
  requestedPath: string;
  extension: string;
  files: string[];
  count: number;
};

type GenerateResponse = {
  basePath: string;
  depth: number;
  createdDirectories: number;
  createdFiles: number;
  deepestPath: string;
};

type HistoryItem = {
  id: number;
  runUser: string;
  runUid: string;
  runGid: string;
  requestedPath: string;
  extension: string;
  requestedAt: string;
  resultCount: number;
  status: string;
};

type ScanRequest = {
  basePath: string;
  signature: string;
  extension: string;
};

type ScanMatch = {
  fileName: string;
  relativePath: string;
};

type ScanResponse = {
  requestedPath: string;
  signature: string;
  extension: string;
  scannedFiles: number;
  matchedFiles: number;
  matches: ScanMatch[];
};

document.querySelector<HTMLDivElement>('#app')!.innerHTML = `
  <main class="container">
    <h1>File Listing App</h1>

    <section class="card">
      <h2>Generate deep file structure</h2>

      <label>
        Base path
        <input id="generateBasePath" value="/input" />
      </label>

      <label>
        Depth
        <input id="depth" type="number" value="10" min="1" />
      </label>

      <label>
        Files per directory
        <input id="filesPerDirectory" type="number" value="1" min="0" />
      </label>

      <label>
        Extension
        <input id="generateExtension" value="txt" />
      </label>

      <button id="generateButton">Generate</button>

      <pre id="generateResult"></pre>
    </section>

    <section class="card">
      <h2>List files</h2>

      <label>
        Path
        <input id="listPath" value="/input" />
      </label>

      <label>
        Extension
        <input id="listExtension" value="txt" />
      </label>

      <button id="listButton">List files</button>

      <p id="fileCount"></p>
      <ul id="fileList"></ul>
    </section>

    <section class="card">
      <h2>Scan files</h2>

      <label>
        Base path
        <input id="scanBasePath" value="/input" />
      </label>

      <label>
        Signature
        <input id="scanSignature" placeholder="SECRET_KEY" />
      </label>

      <label>
        Extension
        <input id="scanExtension" value="txt" />
      </label>

      <button id="scanButton">Scan files</button>

      <p id="scanMessage"></p>
      <div id="scanResults"></div>
    </section>

    <section class="card">
      <h2>History</h2>

      <button id="historyButton">Load history</button>
      <p id="historyError"></p>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>User</th>
            <th>UID</th>
            <th>GID</th>
            <th>Path</th>
            <th>Extension</th>
            <th>Count</th>
            <th>Status</th>
            <th>Requested at</th>
          </tr>
        </thead>
        <tbody id="historyTableBody"></tbody>
      </table>
    </section>
  </main>
`;

const generateButton = document.querySelector<HTMLButtonElement>('#generateButton')!;
const listButton = document.querySelector<HTMLButtonElement>('#listButton')!;
const scanButton = document.querySelector<HTMLButtonElement>('#scanButton')!;
const historyButton = document.querySelector<HTMLButtonElement>('#historyButton')!;

generateButton.addEventListener('click', generateStructure);
listButton.addEventListener('click', listFiles);
scanButton.addEventListener('click', scanFilesFromForm);
historyButton.addEventListener('click', loadHistory);

async function generateStructure(): Promise<void> {
  try {
    const basePath = getInputValue('generateBasePath');
    const depth = Number(getInputValue('depth'));
    const filesPerDirectory = Number(getInputValue('filesPerDirectory'));
    const extension = getInputValue('generateExtension');

    const response = await fetch(`${API_BASE_URL}/api/generate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        basePath,
        depth,
        filesPerDirectory,
        extension
      })
    });

    if (!response.ok) {
      showError('generateResult', await response.text());
      return;
    }

    const data: GenerateResponse = await response.json();

    document.querySelector<HTMLPreElement>('#generateResult')!.textContent =
      JSON.stringify(data, null, 2);
  } catch (error) {
    showError('generateResult', String(error));
  }
}

async function listFiles(): Promise<void> {
  try {
    const path = encodeURIComponent(getInputValue('listPath'));
    const extension = encodeURIComponent(getInputValue('listExtension'));

    const response = await fetch(`${API_BASE_URL}/api/list?path=${path}&extension=${extension}`);

    if (!response.ok) {
      showError('fileCount', await response.text());
      return;
    }

    const data: FileListResponse = await response.json();

    document.querySelector<HTMLParagraphElement>('#fileCount')!.textContent =
      `Found files: ${data.count}`;

    const fileList = document.querySelector<HTMLUListElement>('#fileList')!;
    fileList.replaceChildren();

    data.files.forEach((file) => {
      const listItem = document.createElement('li');
      listItem.textContent = file;
      fileList.appendChild(listItem);
    });
  } catch (error) {
    showError('fileCount', String(error));
  }
}

async function scanFilesFromForm(): Promise<void> {
  const basePath = getInputValue('scanBasePath') || '/input';
  const signature = getInputValue('scanSignature');
  const extension = getInputValue('scanExtension') || 'txt';

  if (!signature) {
    showScanMessage('Signature must not be empty.', true);
    return;
  }

  try {
    showScanMessage('Scanning files...', false);
    clearScanResults();

    const result = await scanFiles({
      basePath,
      signature,
      extension
    });

    showScanMessage(
      `Scanned ${result.scannedFiles} file(s), found ${result.matchedFiles} match(es).`,
      false
    );

    renderScanResults(result);
  } catch (error) {
    showScanMessage(error instanceof Error ? error.message : 'Scan failed.', true);
  }
}

async function scanFiles(request: ScanRequest): Promise<ScanResponse> {
  const response = await fetch(`${API_BASE_URL}/api/scan`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    throw new Error(await response.text());
  }

  return response.json();
}

async function loadHistory(): Promise<void> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/history`);

    if (!response.ok) {
      showError('historyError', await response.text());
      return;
    }

    const history: HistoryItem[] = await response.json();

    const tableBody = document.querySelector<HTMLTableSectionElement>('#historyTableBody')!;
    tableBody.replaceChildren();

    history.forEach((item) => {
      const row = document.createElement('tr');

      appendTableCell(row, item.id);
      appendTableCell(row, item.runUser);
      appendTableCell(row, item.runUid);
      appendTableCell(row, item.runGid);
      appendTableCell(row, item.requestedPath);
      appendTableCell(row, item.extension);
      appendTableCell(row, item.resultCount);
      appendTableCell(row, item.status);
      appendTableCell(row, item.requestedAt);

      tableBody.appendChild(row);
    });
  } catch (error) {
    showError('historyError', String(error));
  }
}

function getInputValue(id: string): string {
  return document.querySelector<HTMLInputElement>(`#${id}`)?.value.trim() ?? '';
}

function showError(elementId: string, message: string): void {
  document.querySelector<HTMLElement>(`#${elementId}`)!.textContent = message;
}

function showScanMessage(message: string, isError: boolean): void {
  const scanMessage = document.querySelector<HTMLParagraphElement>('#scanMessage')!;

  scanMessage.textContent = message;
  scanMessage.className = isError ? 'error-message' : 'success-message';
}

function clearScanResults(): void {
  document.querySelector<HTMLDivElement>('#scanResults')!.replaceChildren();
}

function renderScanResults(result: ScanResponse): void {
  const scanResults = document.querySelector<HTMLDivElement>('#scanResults')!;
  scanResults.replaceChildren();

  if (result.matches.length === 0) {
    const message = document.createElement('p');
    message.textContent = 'No matching files found.';
    scanResults.appendChild(message);
    return;
  }

  const list = document.createElement('ul');

  result.matches.forEach((match) => {
    const listItem = document.createElement('li');

    const fileName = document.createElement('strong');
    fileName.textContent = match.fileName;

    const relativePath = document.createElement('span');
    relativePath.textContent = match.relativePath;

    listItem.appendChild(fileName);
    listItem.appendChild(document.createElement('br'));
    listItem.appendChild(relativePath);

    list.appendChild(listItem);
  });

  scanResults.appendChild(list);
}

function appendTableCell(row: HTMLTableRowElement, value: string | number): void {
  const cell = document.createElement('td');
  cell.textContent = String(value);
  row.appendChild(cell);
}