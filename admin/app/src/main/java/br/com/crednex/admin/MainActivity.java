package br.com.crednex.admin;
import android.app.*;import android.os.*;import android.widget.*;import android.view.*;import android.content.*;import org.json.*;import java.net.*;import java.io.*;
public class MainActivity extends Activity {
 LinearLayout panel; EditText host,login,password; String base,token=""; boolean admin=true; int green=0xff087b38;
 public void onCreate(Bundle b){super.onCreate(b); screen(); host=field("Servidor de teste (http://IP:8765)",false);login=field(admin?"admin":"CPF sem pontuação",false);password=field("Senha",true);
if(android.os.Build.VERSION.SDK_INT>=26){
login.setAutofillHints("username");
password.setAutofillHints("password");
host.setImportantForAutofill(2);
login.setImportantForAutofill(1);
password.setImportantForAutofill(1);
}

SharedPreferences prefs=getSharedPreferences("crednex_access",MODE_PRIVATE);
host.setText(prefs.getString("host","http://127.0.0.1:8765"));
login.setText(prefs.getString("login",""));
CheckBox mostrar=new CheckBox(this);
mostrar.setText("Mostrar senha");
panel.addView(mostrar);
mostrar.setOnCheckedChangeListener((v,checked)->{
password.setInputType(checked?145:129);
password.setSelection(password.length());
});
CheckBox lembrar=new CheckBox(this);
lembrar.setText("Lembrar meu acesso");
lembrar.setChecked(prefs.getBoolean("remember",false));
panel.addView(lembrar);
lembrar.setOnCheckedChangeListener((v,checked)->{
if(!checked)prefs.edit().clear().apply();
});
button("Esqueci minha senha",()->msg("Procure o responsavel autorizado pelo servidor para recuperar seu acesso."));
button("Entrar",()->{base=host.getText().toString().trim().replaceAll("/+$", ""); call("/login",obj("login",login.getText().toString(),"password",password.getText().toString()),r->{if(!r.optString("role").equals(admin?"admin":"client")){msg("Perfil incorreto");return;}
token=r.optString("token","").trim();if(token.isEmpty()){msg("Servidor nao forneceu token");return;}
if(lembrar.isChecked()){
prefs.edit().putString("host",base)
.putString("login",login.getText().toString())
.putBoolean("remember",true).apply();
}else{prefs.edit().clear().apply();}
home();
});});}
 void screen(){ScrollView s=new ScrollView(this);panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(18),dp(20),dp(18),dp(20));panel.setBackgroundColor(0xffffffff);s.addView(panel);setContentView(s);ImageView i=new ImageView(this);i.setImageResource(R.drawable.logo);panel.addView(i,new LinearLayout.LayoutParams(-1,150));label("credneX "+(admin?"ADMIN":"CLIENTES"));label("Ambiente de testes — não use dados reais");}

 int dp(float n){
  return (int)(n*getResources().getDisplayMetrics().density+0.5f);
 }

 android.graphics.drawable.GradientDrawable estilo(
  int cor,int borda,int raio){

  android.graphics.drawable.GradientDrawable d=
   new android.graphics.drawable.GradientDrawable();

  d.setColor(cor);
  d.setCornerRadius(dp(raio));
  d.setStroke(dp(1),borda);
  return d;
 }

 void label(String t){
  TextView v=new TextView(this);
  v.setText(t);
  v.setTextSize(17);
  v.setTextColor(0xff103d2b);
  v.setPadding(dp(4),dp(10),dp(4),dp(10));
  panel.addView(v);
 }

 EditText field(String h,boolean secret){
  EditText e=new EditText(this);
  e.setHint(h);
  e.setTextColor(0xff103d2b);
  e.setHintTextColor(0xff68796e);
  e.setTextSize(16);
  e.setSingleLine(true);

  if(secret)e.setInputType(129);

  e.setPadding(dp(14),dp(12),dp(14),dp(12));

  e.setBackground(estilo(
   0xffffffff,0xffc5d5c9,12));

  LinearLayout.LayoutParams lp=
   new LinearLayout.LayoutParams(-1,-2);

  lp.setMargins(0,dp(6),0,dp(6));
  panel.addView(e,lp);

  return e;
 }

 void button(String t,Runnable r){
  Button b=new Button(this);

  b.setText(t);
  b.setAllCaps(false);
  b.setTextColor(0xffffffff);
  b.setTextSize(15);
  b.setGravity(android.view.Gravity.CENTER);
  b.setMinHeight(dp(52));
  b.setPadding(dp(12),dp(10),dp(12),dp(10));

  b.setBackgroundTintList(null);

  b.setBackground(estilo(
   0xff103d2b,0xffc4a24e,14));

  LinearLayout.LayoutParams lp=
   new LinearLayout.LayoutParams(-1,-2);

  lp.setMargins(0,dp(5),0,dp(5));

  panel.addView(b,lp);
  b.setOnClickListener(v->r.run());
 }

 void msg(String t){new AlertDialog.Builder(this).setMessage(t).setPositiveButton("OK",null).show();}
 JSONObject obj(String... a){JSONObject o=new JSONObject();try{for(int i=0;i<a.length;i+=2)o.put(a[i],a[i+1]);}catch(Exception e){}return o;}
 interface Done{void run(JSONObject o);}
 void call(String path,JSONObject data,Done done){new Thread(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(base+path).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setInstanceFollowRedirects(false);if(!path.equals("/login") && token!=null && !token.isEmpty()){c.setRequestProperty("Authorization","Bearer "+token.trim());}if(data!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(data.toString().getBytes("UTF-8"));}}int status=c.getResponseCode();InputStream in=status<400?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);in.close();JSONObject o=new JSONObject(out.toString("UTF-8"));c.disconnect();runOnUiThread(()->{if(status>=400)msg(o.optString("erro","Falha"));else done.run(o);});}catch(Exception e){runOnUiThread(()->msg("Falha de conexão. Confira o servidor."));}}).start();}

 void consulta(String titulo,String rota){
  call(rota,null,r->{
   screen();
   label(titulo);
   mostrarDados(r);
   button("Voltar",()->home());
  });
 }

 String moeda(long valor){
  return java.text.NumberFormat.getCurrencyInstance(
   java.util.Locale.forLanguageTag("pt-BR")
  ).format(valor/100.0);
 }

 void mostrarDados(JSONObject dados){
  java.util.Iterator<String> chaves=dados.keys();
  while(chaves.hasNext()){
   String k=chaves.next();
   Object v=dados.opt(k);

   if(v instanceof org.json.JSONArray){
    org.json.JSONArray a=(org.json.JSONArray)v;
    if(a.length()==0)label("Nenhum registro encontrado");
    for(int i=0;i<a.length();i++){
     label("Registro "+(i+1));
     JSONObject item=a.optJSONObject(i);
     if(item!=null)mostrarDados(item);
    }
   }else{
    String nome=k.replace("_"," ");
    String texto=String.valueOf(v);

    if(k.matches("capital|principal|total|amount|paid|balance|disponivel|entradas|saidas|recebido|emprestado|saldo_devedor|atrasado|inadimplencia|juros_contratados|despesas_pagas|despesas_pendentes|estimated_value")){
     texto=moeda(dados.optLong(k));
    }

    label(nome.toUpperCase(java.util.Locale.ROOT)+": "+texto);
   }
  }
 }


 void formulario(String titulo,String rota,String[] nomes,String[] chaves,String[] tipos){
  screen();
  label(titulo);

  EditText[] campos=new EditText[nomes.length];

  for(int i=0;i<nomes.length;i++){
   campos[i]=field(nomes[i],tipos[i].equals("senha"));
  }

  button("Salvar operacao",()->{
   try{
    JSONObject dados=new JSONObject();

    for(int i=0;i<campos.length;i++){
     String v=campos[i].getText().toString().trim();

     if(v.isEmpty())throw new Exception("Campo obrigatorio: "+nomes[i]);

     if(tipos[i].equals("dinheiro")){
      java.math.BigDecimal n=new java.math.BigDecimal(v.replace(",","."));
      dados.put(chaves[i],n.movePointRight(2).longValueExact());
     }else if(tipos[i].equals("inteiro")){
      dados.put(chaves[i],Integer.parseInt(v));
     }else if(tipos[i].equals("decimal")){
      dados.put(chaves[i],Double.parseDouble(v.replace(",",".")));
     }else{
      dados.put(chaves[i],v);
     }
    }

    if(rota.equals("/admin/payment")){
     if(!dados.has("reference")){
      dados.put("reference",java.util.UUID.randomUUID().toString());
     }
    }

    new AlertDialog.Builder(this)
     .setTitle("Confirmar operacao")
     .setMessage("Confira os dados antes de confirmar.")
     .setPositiveButton("Confirmar",(a,b)->{
      call(rota,dados,r->{
       home();
       msg("Operacao registrada com sucesso");
      });
     })
     .setNegativeButton("Cancelar",null)
     .show();

   }catch(Exception e){
    msg("Confira os campos. "+e.getMessage());
   }
  });

  button("Voltar",()->home());
 }

 void operacoes(){
  screen();
  label("OPERACOES ADMINISTRATIVAS");

  button("Definir capital inicial",()->formulario(
   "Capital inicial","/admin/capital",
   new String[]{"Valor em reais"},
   new String[]{"capital"},
   new String[]{"dinheiro"}));

  button("Novo emprestimo",()->formulario(
   "Conceder emprestimo","/admin/loan",
   new String[]{"ID do cliente","Valor em reais","Juros mensais (%)","Quantidade de parcelas","Primeiro vencimento AAAA-MM-DD"},
   new String[]{"client_id","principal","interest_rate","installments","first_due"},
   new String[]{"inteiro","dinheiro","decimal","inteiro","texto"}));

  button("Receber parcela",()->formulario(
   "Recebimento","/admin/payment",
   new String[]{"ID da parcela","Valor recebido em reais","Forma: pix, dinheiro, transferencia ou boleto","Referencia unica do pagamento"},
   new String[]{"installment_id","amount","method","reference"},
   new String[]{"inteiro","dinheiro","texto","texto"}));

  button("Aportes e retiradas",()->formulario(
   "Movimentacao financeira","/admin/cash",
   new String[]{"Tipo: entrada ou saida","Descricao","Valor em reais"},
   new String[]{"kind","description","amount"},
   new String[]{"texto","texto","dinheiro"}));

  button("Cadastrar despesa",()->formulario(
   "Nova despesa","/admin/expense",
   new String[]{"Descricao","Categoria","Valor em reais","Vencimento AAAA-MM-DD"},
   new String[]{"description","category","amount","due_date"},
   new String[]{"texto","texto","dinheiro","texto"}));

  button("Pagar despesa",()->formulario(
   "Baixa de despesa","/admin/expense/pay",
   new String[]{"ID da despesa"},
   new String[]{"id"},
   new String[]{"inteiro"}));

  button("Registrar contrato",()->formulario(
   "Novo contrato","/admin/contract",
   new String[]{"ID do emprestimo","Conteudo do contrato"},
   new String[]{"loan_id","content"},
   new String[]{"inteiro","texto"}));

  button("Registrar garantia",()->formulario(
   "Nova garantia","/admin/guarantee",
   new String[]{"ID do emprestimo","Tipo de garantia","Descricao do bem","Valor estimado em reais"},
   new String[]{"loan_id","kind","description","estimated_value"},
   new String[]{"inteiro","texto","texto","dinheiro"}));

  button("Editar cliente",()->formulario(
   "Editar cadastro","/admin/client/update",
   new String[]{"ID do cliente","Nome completo","CPF"},
   new String[]{"id","name","cpf"},
   new String[]{"inteiro","texto","texto"}));

  button("Bloquear ou reativar cliente",()->formulario(
   "Situacao do cliente","/admin/client/status",
   new String[]{"ID do cliente","0 para bloquear ou 1 para ativar"},
   new String[]{"id","active"},
   new String[]{"inteiro","inteiro"}));

  button("Redefinir senha",()->formulario(
   "Nova senha do cliente","/admin/client/password",
   new String[]{"ID do cliente","Nova senha (12+ caracteres)"},
   new String[]{"id","password"},
   new String[]{"inteiro","senha"}));

  button("Voltar",()->home());
 }


 void barraFinanceira(String titulo,long valor,long maximo,int cor){
  label(titulo+" — "+moeda(valor));

  LinearLayout fundo=new LinearLayout(this);
  fundo.setOrientation(0);
  fundo.setBackgroundColor(0xffdce8df);

  android.view.View barra=new android.view.View(this);
  barra.setBackgroundColor(cor);

  float proporcao=maximo>0?
   Math.max(0f,Math.min(1f,(float)valor/maximo)):0f;

  fundo.addView(barra,new LinearLayout.LayoutParams(
   0,-1,proporcao));

  android.view.View restante=new android.view.View(this);

  fundo.addView(restante,new LinearLayout.LayoutParams(
   0,-1,1f-proporcao));

  LinearLayout.LayoutParams lp=
   new LinearLayout.LayoutParams(-1,36);

  lp.setMargins(0,0,0,20);
  panel.addView(fundo,lp);
 }

 void graficosFinanceiros(){
  call("/admin/reports",null,r->{
   screen();
   label("INDICADORES FINANCEIROS");

   String[] nomes={
    "Capital inicial",
    "Capital disponivel",
    "Emprestimos concedidos",
    "Juros contratados",
    "Recebimentos",
    "Despesas pagas",
    "Despesas pendentes",
    "Inadimplencia"
   };

   String[] chaves={
    "capital","disponivel","principal",
    "juros_contratados","recebido",
    "despesas_pagas","despesas_pendentes",
    "inadimplencia"
   };

   int[] cores={
    0xff17613c,0xff218c55,
    0xff2878b5,0xffc4a24e,
    0xff218c55,0xffd47c32,
    0xffa77732,0xffc43d3d
   };

   long maximo=1;

   for(String chave:chaves){
    maximo=Math.max(maximo,r.optLong(chave));
   }

   for(int i=0;i<chaves.length;i++){
    barraFinanceira(nomes[i],
     r.optLong(chaves[i]),maximo,cores[i]);
   }

   button("Grafico mensal",()->graficosMensais());
   button("Atualizar",()->graficosFinanceiros());
   button("Voltar",()->home());
  });
 }

 void graficosMensais(){
  call("/admin/charts",null,r->{
   screen();
   label("MOVIMENTACAO MENSAL");
   label("Verde: entradas | Dourado: saidas");

   org.json.JSONArray meses=r.optJSONArray("monthly");

   if(meses==null || meses.length()==0){
    label("Nenhuma movimentacao registrada");
   }else{
    long maximo=1;

    for(int i=0;i<meses.length();i++){
     JSONObject m=meses.optJSONObject(i);
     if(m==null)continue;

     maximo=Math.max(maximo,m.optLong("entradas"));
     maximo=Math.max(maximo,m.optLong("saidas"));
    }

    for(int i=0;i<meses.length();i++){
     JSONObject m=meses.optJSONObject(i);
     if(m==null)continue;

     long entrada=m.optLong("entradas");
     long saida=m.optLong("saidas");

     label("COMPETENCIA: "+m.optString("mes"));

     barraFinanceira("Entradas",
      entrada,maximo,0xff218c55);

     barraFinanceira("Saidas",
      saida,maximo,0xffc4a24e);

     label("Movimentacao liquida: "+
      moeda(entrada-saida));
    }
   }

   button("Atualizar",()->graficosMensais());
   button("Voltar",()->graficosFinanceiros());
  });
 }


 void centralPix(){
  call("/pix/payment",null,r->{
   screen();
   label("CENTRAL DE PAGAMENTOS PIX");

   label("Beneficiario: "+r.optString("beneficiary"));
   label("Chave Pix: "+r.optString("pix"));

   if(!r.optBoolean("configured")){
    label("Chave Pix ainda nao configurada");
   }

   org.json.JSONArray parcelas=r.optJSONArray("installments");

   if(parcelas==null || parcelas.length()==0){
    label("Nenhuma parcela pendente");
   }else{
    for(int i=0;i<parcelas.length();i++){
     JSONObject item=parcelas.optJSONObject(i);
     if(item==null)continue;

     int id=item.optInt("id");

     label("Parcela: "+item.optInt("number"));
     label("Vencimento: "+item.optString("due_date"));
     label("Saldo: "+moeda(item.optLong("balance")));

     button("Gerar Pix - Parcela "+id,
      ()->exibirPix(id));
    }
   }

   if(admin){
    button("Configurar municipio",()->{
     screen();
     EditText cidade=field("Municipio do beneficiario",false);

     button("Salvar",()->call("/admin/pix/city",
      obj("city",cidade.getText().toString()),
      x->{centralPix();msg("Municipio atualizado");}));

     button("Voltar",()->centralPix());
    });
   }

   button("Voltar",()->home());
  });
 }

 void exibirPix(int id){
  call("/pix/code/"+id,null,r->{
   screen();
   label("PAGAMENTO PIX");
   label("Beneficiario: "+r.optString("beneficiary"));
   label("Municipio: "+r.optString("city"));
   label("Valor: "+moeda(r.optLong("amount")));

   String codigo=r.optString("payload");

   if(codigo.isEmpty()){
    label("Codigo Pix indisponivel");
    button("Voltar",()->centralPix());
    return;
   }

   try{
    java.util.Map<com.google.zxing.EncodeHintType,Object> hints=
     new java.util.HashMap<>();

    hints.put(com.google.zxing.EncodeHintType.MARGIN,4);
    hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET,"UTF-8");

    com.google.zxing.common.BitMatrix matriz=
     new com.google.zxing.qrcode.QRCodeWriter().encode(
      codigo,
      com.google.zxing.BarcodeFormat.QR_CODE,
      600,600,hints);

    android.graphics.Bitmap imagem=
     android.graphics.Bitmap.createBitmap(
      600,600,android.graphics.Bitmap.Config.ARGB_8888);

    for(int y=0;y<600;y++){
     for(int x=0;x<600;x++){
      imagem.setPixel(x,y,
       matriz.get(x,y)?0xff000000:0xffffffff);
     }
    }

    ImageView qr=new ImageView(this);
    qr.setImageBitmap(imagem);
    qr.setScaleType(ImageView.ScaleType.FIT_CENTER);
    panel.addView(qr,new LinearLayout.LayoutParams(-1,650));

   }catch(Exception e){
    label("Nao foi possivel gerar a imagem do QR Code");
   }

   button("Copiar Pix Copia e Cola",()->{
    ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE))
     .setPrimaryClip(ClipData.newPlainText("Pix",codigo));

    msg("Codigo copiado. Confira o beneficiario no banco.");
   });

   label("Confira o beneficiario e o valor antes de pagar.");
   label("A confirmacao do pagamento depende da conferencia do administrador.");

   button("Voltar",()->centralPix());
  });
 }


 void alterarSenha(){
  screen();
  label("SEGURANCA DA CONTA");

  EditText atual=field("Senha atual",true);
  EditText nova=field("Nova senha (12+ caracteres)",true);
  EditText confirmar=field("Confirmar nova senha",true);

  button("Alterar senha",()->{
   String a=atual.getText().toString();
   String n=nova.getText().toString();
   String c=confirmar.getText().toString();

   if(n.length()<12 || n.length()>128){
    msg("A senha deve ter entre 12 e 128 caracteres");
    return;
   }

   if(!n.equals(c)){
    msg("As senhas nao conferem");
    return;
   }

   call("/password/change",
    obj("current_password",a,"new_password",n),
    r->{
     token="";
     new AlertDialog.Builder(this)
      .setTitle("Senha atualizada")
      .setMessage("Entre novamente com sua nova senha.")
      .setCancelable(false)
      .setPositiveButton("Entrar",(x,y)->recreate())
      .show();
    });
  });

  button("Voltar",()->home());
 }

 void home(){
  screen();
  button("Alterar minha senha",()->alterarSenha());
  button("Central Pix e QR Code",()->centralPix());

  if(admin){
   label("PAINEL ADMINISTRATIVO V2.0");
   button("Graficos financeiros",()->graficosFinanceiros());
   button("Operacoes administrativas",()->operacoes());

   button("Dashboard financeiro",()->consulta("Dashboard","/admin/dashboard"));

   button("Gerenciar clientes",()->consulta("Clientes","/admin/clients"));

   button("Cadastrar cliente",()->{
    screen();
    EditText cpf=field("CPF",false);
    EditText name=field("Nome completo",false);
    EditText pw=field("Senha inicial (12+ caracteres)",true);

    button("Salvar",()->call("/admin/client",
     obj("cpf",cpf.getText().toString(),
     "name",name.getText().toString(),
     "password",pw.getText().toString()),
     r->{home();msg("Cliente cadastrado");}));

    button("Voltar",()->home());
   });

   button("Capital e saldo disponivel",()->consulta("Capital","/admin/finance"));

   button("Emprestimos",()->consulta("Emprestimos","/loans"));

   button("Parcelamentos",()->consulta("Parcelamentos","/installments"));

   button("Recebimentos",()->consulta("Recebimentos","/payments"));

   button("Despesas e contas a pagar",()->consulta("Despesas","/admin/expenses"));

   button("Fluxo de caixa",()->consulta("Fluxo de caixa","/admin/cashflow"));

   button("Contratos",()->consulta("Contratos","/contracts"));

   button("Garantias",()->consulta("Garantias","/guarantees"));

   button("Fiscal e relatorios",()->consulta("Relatorios gerenciais","/admin/reports"));

   button("Demonstrativo mensal",()->consulta("Movimentacao mensal","/admin/charts"));

   button("Configurar Pix",()->{
    screen();
    EditText key=field("Chave Pix",false);
    EditText name=field("Beneficiario",false);

    button("Salvar",()->call("/admin/pix",
     obj("pix",key.getText().toString(),
     "beneficiary",name.getText().toString()),
     r->{home();msg("Chave atualizada");}));

    button("Voltar",()->home());
   });

  }else{

   label("CENTRAL DO CLIENTE V2.0");

   button("Meu cadastro",()->consulta("Meu cadastro","/me"));

   button("Meus emprestimos",()->consulta("Meus emprestimos","/loans"));

   button("Minhas parcelas",()->consulta("Minhas parcelas","/installments"));

   button("Historico de pagamentos",()->consulta("Pagamentos","/payments"));

   button("Meus contratos",()->consulta("Meus contratos","/contracts"));

   button("Minhas garantias",()->consulta("Minhas garantias","/guarantees"));

   button("Parcelas para pagamento",()->consulta("Parcelas pendentes","/pix/payment"));
  }

  button("Chave Pix",()->call("/pix",null,r->{
   String key=r.optString("pix");

   new AlertDialog.Builder(this)
    .setTitle("Chave Pix")
    .setMessage(r.optString("beneficiary")+"\n"+key)
    .setPositiveButton("Copiar",(x,y)->{
     ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE))
      .setPrimaryClip(ClipData.newPlainText("Pix",key));
    })
    .setNegativeButton("Fechar",null)
    .show();
  }));

  button("Sair",()->call("/logout",obj(),r->{
   token="";
   recreate();
  }));
 }

}
