import { useEffect, useState } from "react";

const API_URL = "http://localhost:8080/api/v1/pautas";

const emptyPauta = { titulo: "", descricao: "" };
const emptySessao = { duracaoSegundos: 60 };
const emptyVoto = { associadoId: "", cpf: "", opcao: "SIM" };

function formatDate(value) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "medium"
  }).format(new Date(value));
}

function getErrorMessage(error) {
  if (error?.status === 404) {
    return "CPF inválido no serviço externo. Tente outro CPF.";
  }

  if (error?.status === 422 && error?.message === "Associado nao esta apto a votar") {
    return "CPF validado, mas o associado foi marcado como inapto para votar.";
  }

  if (error?.status === 422 && error?.message === "Associado ja votou nesta pauta") {
    return "Este associado já registrou voto nesta pauta.";
  }

  if (error?.status === 422 && error?.message === "A sessao de votacao esta encerrada") {
    return "A sessão desta pauta já foi encerrada.";
  }

  return error?.message || "Falha ao processar a solicitação.";
}

async function request(path = "", options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    headers: {
      "Content-Type": "application/json"
    },
    ...options
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    const error = new Error(body.message || "Falha na requisição");
    error.status = response.status;
    throw error;
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

function App() {
  const [pautas, setPautas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [globalError, setGlobalError] = useState("");
  const [globalSuccess, setGlobalSuccess] = useState("");
  const [pautaMessages, setPautaMessages] = useState({});
  const [pautaForm, setPautaForm] = useState(emptyPauta);
  const [sessaoForms, setSessaoForms] = useState({});
  const [votoForms, setVotoForms] = useState({});

  function clearGlobalMessages() {
    setGlobalError("");
    setGlobalSuccess("");
  }

  function setMessageForPauta(pautaId, type, message) {
    setPautaMessages((current) => ({
      ...current,
      [pautaId]: { type, message }
    }));
  }

  function clearMessageForPauta(pautaId) {
    setPautaMessages((current) => {
      const next = { ...current };
      delete next[pautaId];
      return next;
    });
  }

  async function loadPautas() {
    setLoading(true);
    setGlobalError("");
    try {
      const data = await request();
      setPautas(data);
    } catch (err) {
      setGlobalError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadPautas();
  }, []);

  async function handleCreatePauta(event) {
    event.preventDefault();
    clearGlobalMessages();

    try {
      await request("", {
        method: "POST",
        body: JSON.stringify(pautaForm)
      });
      setPautaForm(emptyPauta);
      setGlobalSuccess("Pauta criada com sucesso.");
      await loadPautas();
    } catch (err) {
      setGlobalError(getErrorMessage(err));
    }
  }

  async function handleOpenSession(pautaId) {
    clearGlobalMessages();
    clearMessageForPauta(pautaId);
    const form = sessaoForms[pautaId] || emptySessao;

    try {
      await request(`/${pautaId}/sessao`, {
        method: "POST",
        body: JSON.stringify(form)
      });
      setMessageForPauta(pautaId, "success", "Sessão aberta com sucesso.");
      await loadPautas();
    } catch (err) {
      setMessageForPauta(pautaId, "error", getErrorMessage(err));
    }
  }

  async function handleVote(pautaId, event) {
    event.preventDefault();
    clearGlobalMessages();
    clearMessageForPauta(pautaId);
    const form = votoForms[pautaId] || emptyVoto;

    try {
      await request(`/${pautaId}/votos`, {
        method: "POST",
        body: JSON.stringify(form)
      });
      setVotoForms((current) => ({
        ...current,
        [pautaId]: emptyVoto
      }));
      setMessageForPauta(pautaId, "success", "Voto registrado com sucesso.");
      await loadPautas();
    } catch (err) {
      setMessageForPauta(pautaId, "error", getErrorMessage(err));
    }
  }

  return (
    <main className="page-shell">
      <section className="topbar">
        <div>
          <h1>Sistema de Votação</h1>
        </div>
        <button className="secondary-button" type="button" onClick={loadPautas}>
          Atualizar pautas
        </button>
      </section>

      <section className="panel">
        <div className="section-header">
          <h2>Cadastro de pauta</h2>
        </div>

        <form className="simple-form" onSubmit={handleCreatePauta}>
          <label>
            Título
            <input
              value={pautaForm.titulo}
              onChange={(event) =>
                setPautaForm((current) => ({
                  ...current,
                  titulo: event.target.value
                }))
              }
              placeholder="Ex.: Aprovação do balanço anual"
              required
            />
          </label>
          <label className="full-width">
            Descrição
            <textarea
              value={pautaForm.descricao}
              onChange={(event) =>
                setPautaForm((current) => ({
                  ...current,
                  descricao: event.target.value
                }))
              }
              placeholder="Descreva o objetivo da pauta"
              rows="3"
              required
            />
          </label>
          <div className="full-width form-actions">
            <button type="submit">Cadastrar pauta</button>
          </div>
        </form>
      </section>

      {(globalError || globalSuccess) && (
        <section className={`feedback ${globalError ? "error" : "success"}`}>
          {globalError || globalSuccess}
        </section>
      )}

      {loading ? (
        <section className="panel">Carregando pautas...</section>
      ) : pautas.length === 0 ? (
        <section className="panel">Nenhuma pauta cadastrada até o momento.</section>
      ) : (
        <section className="section-header list-header">
          <h2>Pautas registradas</h2>
        </section>
      )}

      {!loading && pautas.length > 0 && (
        <section className="cards">
          {pautas.map((pauta) => {
            const sessaoForm = sessaoForms[pauta.id] || emptySessao;
            const votoForm = votoForms[pauta.id] || emptyVoto;
            const pautaMessage = pautaMessages[pauta.id];

            return (
              <article className="panel card" key={pauta.id}>
                <div className="card-title-row">
                  <div>
                    <h3>{pauta.titulo}</h3>
                    <p className="muted-text">Pauta #{pauta.id}</p>
                  </div>
                  <span
                    className={`status ${pauta.sessao?.aberta ? "open" : "closed"}`}
                  >
                    {pauta.sessao?.aberta ? "Sessão aberta" : "Sessão fechada"}
                  </span>
                </div>

                <p className="description">{pauta.descricao}</p>
                <p className="muted-text">Criada em {formatDate(pauta.criadaEm)}</p>

                <div className="result-grid">
                  <div>
                    <span>SIM</span>
                    <strong>{pauta.resultado?.votosSim ?? 0}</strong>
                  </div>
                  <div>
                    <span>NÃO</span>
                    <strong>{pauta.resultado?.votosNao ?? 0}</strong>
                  </div>
                  <div>
                    <span>Total</span>
                    <strong>{pauta.resultado?.totalVotos ?? 0}</strong>
                  </div>
                  <div>
                    <span>Resultado</span>
                    <strong>{pauta.resultado?.situacao ?? "PENDENTE"}</strong>
                  </div>
                </div>

                <div className="action-block">
                  <div>
                    <h4>Sessão</h4>
                    <p className="muted-text">Início: {formatDate(pauta.sessao?.abertaEm)}</p>
                    <p className="muted-text">
                      Encerramento: {formatDate(pauta.sessao?.encerraEm)}
                    </p>
                  </div>
                  <div className="inline-actions">
                    <input
                      type="number"
                      min="1"
                      value={sessaoForm.duracaoSegundos}
                      onChange={(event) =>
                        setSessaoForms((current) => ({
                          ...current,
                          [pauta.id]: {
                            duracaoSegundos: Number(event.target.value)
                          }
                        }))
                      }
                    />
                    <button type="button" onClick={() => handleOpenSession(pauta.id)}>
                      Abrir sessão
                    </button>
                  </div>
                </div>

                <form
                  className="simple-form vote-form"
                  onSubmit={(event) => handleVote(pauta.id, event)}
                >
                  <h4>Registrar voto</h4>
                  <input
                    value={votoForm.associadoId}
                    onChange={(event) =>
                      setVotoForms((current) => ({
                        ...current,
                        [pauta.id]: {
                          ...votoForm,
                          associadoId: event.target.value
                        }
                      }))
                    }
                    placeholder="Identificador do associado"
                    required
                  />
                  <input
                    value={votoForm.cpf}
                    onChange={(event) =>
                      setVotoForms((current) => ({
                        ...current,
                        [pauta.id]: {
                          ...votoForm,
                          cpf: event.target.value
                        }
                      }))
                    }
                    placeholder="CPF do associado"
                    required
                  />
                  <select
                    value={votoForm.opcao}
                    onChange={(event) =>
                      setVotoForms((current) => ({
                        ...current,
                        [pauta.id]: {
                          ...votoForm,
                          opcao: event.target.value
                        }
                      }))
                    }
                  >
                    <option value="SIM">Sim</option>
                    <option value="NAO">Não</option>
                  </select>
                  <div className="form-actions">
                    <button type="submit">Votar</button>
                  </div>
                </form>

                {pautaMessage && (
                  <div className={`inline-feedback ${pautaMessage.type}`}>
                    {pautaMessage.message}
                  </div>
                )}
              </article>
            );
          })}
        </section>
      )}
    </main>
  );
}

export default App;
