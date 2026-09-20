# Audio Tools — Changelog

## 0.12.0 — Suporte por email

### Suporte
- Formulário de suporte, reclamações e sugestões agora abre o aplicativo de email do utilizador.
- O destinatário é pré-configurado como nexaurenstore@gmail.com.
- O email é preenchido automaticamente com a mensagem e os dados disponíveis da conta Firebase.
- O utilizador confirma o envio no Gmail, Outlook ou outro aplicativo de email instalado.
- Removida a dependência de Resend e do envio de suporte pelo Cloudflare Worker.
- Mantidos Firebase Authentication, PayPal, planos e entitlement sem alterações.

## 0.11.0 — Home e armazenamento

### Interface
- Home focada exclusivamente nas seis ferramentas.
- Grelha 3×2 de ferramentas na Home.
- Menu único no canto superior direito.
- Favoritos, partilha e abertura diretamente no card.
- Nova paleta de cores mais viva e contrastada.

### Ferramentas
- Circuito animado durante processamento.
- Fluxo consistente: selecionar → processar → resultado → guardar.
- Partilha de resultado corrigida para URI/ficheiro temporário.

### Armazenamento
- Resultados finais saem da área privada do app.
- Pasta escolhida pelo utilizador através do Storage Access Framework.
- Cada ferramenta cria automaticamente a sua própria pasta:
  <Ferramenta> - Nexauren
- A pasta fica no armazenamento partilhado escolhido pelo utilizador.

### Organização
- Recentes, favoritos, histórico e estatísticas deixam de ocupar a Home.
- Centro de armazenamento disponível no Menu e nas Definições.


## 0.10.0 — Nova experiência

### Interface
- Home renovada com cabeçalho mais elegante, pesquisa rápida e estado do plano.
- Cards de ferramentas com ícones, estado FREE/PRO/PREMIUM, favoritos e cadeado.
- Menu central com todas as áreas principais.
- Navegação inferior entre Home, Tools, Favoritos, Planos e Me.
- Visual claro, espaçamento consistente, toques maiores e estados vazios.

### Organização
- Favoritos guardados localmente.
- Histórico de utilizações guardado localmente.
- Estatísticas pessoais de utilização.
- Página Me com foto, nome, UID Firebase, email, fornecedor e datas da conta.
- Página de suporte com suporte, reclamações e sugestões.

### Ferramentas
- Histórico registado ao abrir uma ferramenta.
- Botão de partilha para resultados gerados.
- Ferramentas continuam organizadas como um único produto de áudio, sem categorias de navegação.

### Planos
- Free, Pro e Premium continuam controlados pelo sistema atual de entitlement.
- Ferramentas pagas continuam bloqueadas para contas sem acesso.
- Upgrade apresenta comparação dos três planos e identifica o plano atual.

### Conta
- Firebase Authentication e Google Sign-In preservados.
- Dados do perfil continuam sincronizados com Firestore.

### Atualizações
- versionName controlado no projeto.
- versionCode automático no CI através do número da execução do GitHub Actions.
- updates.json atualizado para a nova versão.
- Fluxo existente de verificação, download e instalação continua ativo.

## 0.9.0 — Planos por assinatura

- Free sem expiração.
- Pro por $5/mês.
- Premium por $10/mês.
- Bloqueio de ferramentas por entitlement.
- PayPal por assinatura.
- Remoção do sistema antigo de créditos e compras únicas.
