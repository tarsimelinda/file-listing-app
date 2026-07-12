import './style.css';

const API_BASE_URL = 'http://localhost:8080';

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
      <h2>History</h2>

      <button id="historyButton">Load history</button>

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>User</th>
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
const historyButton = document.querySelector<HTMLButtonElement>('#historyButton')!;

generateButton.addEventListener('click', generateStructure);
listButton.addEventListener('click', listFiles);
historyButton.addEventListener('click', loadHistory);

async function generateStructure(): Promise<void> {
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
}

async function listFiles(): Promise<void> {
  const path = encodeURIComponent(getInputValue('listPath'));
  const extension = encodeURIComponent(getInputValue('listExtension'));

  const response = await fetch(`${API_BASE_URL}/api/list?path=${path}&extension=${extension}`);

  if (!response.ok) {
    const errorText = await response.text();
    document.querySelector<HTMLParagraphElement>('#fileCount')!.textContent = errorText;
    return;
  }

  const data: FileListResponse = await response.json();

  document.querySelector<HTMLParagraphElement>('#fileCount')!.textContent =
    `Found files: ${data.count}`;

  const fileList = document.querySelector<HTMLUListElement>('#fileList')!;
  fileList.innerHTML = '';

  data.files.forEach((file) => {
    const listItem = document.createElement('li');
    listItem.textContent = file;
    fileList.appendChild(listItem);
  });
}

async function loadHistory(): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/history`);

  if (!response.ok) {
    return;
  }

  const history: HistoryItem[] = await response.json();

  const tableBody = document.querySelector<HTMLTableSectionElement>('#historyTableBody')!;
  tableBody.innerHTML = '';

  history.forEach((item) => {
    const row = document.createElement('tr');

    row.innerHTML = `
      <td>${item.id}</td>
      <td>${item.runUser}</td>
      <td>${item.requestedPath}</td>
      <td>${item.extension}</td>
      <td>${item.resultCount}</td>
      <td>${item.status}</td>
      <td>${item.requestedAt}</td>
    `;

    tableBody.appendChild(row);
  });
}

function getInputValue(id: string): string {
  return document.querySelector<HTMLInputElement>(`#${id}`)!.value;
}

function showError(elementId: string, message: string): void {
  document.querySelector<HTMLElement>(`#${elementId}`)!.textContent = message;
}