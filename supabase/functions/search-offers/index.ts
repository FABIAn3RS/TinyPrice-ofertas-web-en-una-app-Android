const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
};

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });

const extractPrice = (text: string | undefined): string | null => {
  if (!text) return null;
  const match = text.match(
    /(?:US\$|USD\s*|\$\s*|EUR\s*|€\s*|GBP\s*|£\s*)\s*\d[\d,.]*(?:\s*(?:USD|EUR|GBP))?/i,
  );
  return match?.[0].trim() ?? null;
};

Deno.serve(async (req) => {
  // CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    if (req.method !== "POST") {
      return json({ error: "Method not allowed" }, 405);
    }

    const { query } = await req.json();
    console.log("[search-offers] query recibida:", query);

    if (!query || typeof query !== "string") {
      return json({ error: "Missing or invalid query parameter" }, 400);
    }

    const tinyfishApiKey = Deno.env.get("TINYFISH_API_KEY");
    console.log("[search-offers] TINYFISH_API_KEY presente:", !!tinyfishApiKey);
    if (!tinyfishApiKey) {
      console.error("[search-offers] Falta el secret TINYFISH_API_KEY");
      return json(
        { error: "Server configuration error: Missing TINYFISH_API_KEY" },
        500,
      );
    }

    const encodedQuery = encodeURIComponent(query);
    const tinyfishUrl =
      `https://api.search.tinyfish.ai?query=${encodedQuery}`;

    let rawData: any = {};
    try {
      const tinyfishRes = await fetch(tinyfishUrl, {
        method: "GET",
        headers: {
          "X-API-Key": tinyfishApiKey,
        },
      });

      console.log("[search-offers] TinyFish status:", tinyfishRes.status);

      if (tinyfishRes.ok) {
        rawData = await tinyfishRes.json();
      } else {
        const errText = await tinyfishRes.text();
        console.error(
          "[search-offers] TinyFish respondió error:",
          tinyfishRes.status,
          errText.slice(0, 500),
        );
      }
    } catch (fetchErr) {
      // Fallo de red / DNS / URL inválida: no tumbamos toda la función
      const fe = fetchErr as Error;
      console.error("[search-offers] Falló el fetch a TinyFish:", fe.message);
    }

    const rawItems = rawData.results || rawData.items || rawData.offers || [];
    const fetchPrices = new Map<string, string>();
    if (rawItems.length > 0) {
      const candidateItems = rawItems.filter((item: any) => {
        const url = String(item.url || item.sourceUrl || "");
        return /^https?:\/\//i.test(url) &&
          !/wikipedia\.org|youtube\.com/i.test(url);
      }).slice(0, 5);

      if (candidateItems.length > 0) {
        try {
          const fetchRes = await fetch("https://api.fetch.tinyfish.ai", {
            method: "POST",
            headers: {
              "X-API-Key": tinyfishApiKey,
              "Content-Type": "application/json",
            },
            body: JSON.stringify({
              urls: candidateItems.map((item: any) => item.url || item.sourceUrl),
              format: "markdown",
              ttl: 0,
              per_url_timeout_ms: 20000,
              purpose: `Find the current product price for ${query}`,
            }),
          });

          if (fetchRes.ok) {
            const fetched = await fetchRes.json();
            const queryWords = query.toLowerCase().match(/[a-z0-9]+/g) || [];
            for (const page of fetched.results || []) {
              const sourceItem = candidateItems.find((item: any) =>
                (item.url || item.sourceUrl) === page.url
              );
              const pageTitle = `${page.title || ""} ${sourceItem?.title || ""}`
                .toLowerCase();
              if (!queryWords.every((word: string) => pageTitle.includes(word))) {
                continue;
              }
              const price = extractPrice(page.text || "");
              if (price) fetchPrices.set(page.url, price);
            }
          } else {
            console.error("[search-offers] TinyFish Fetch status:", fetchRes.status);
          }
        } catch (fetchErr) {
          console.error("[search-offers] TinyFish Fetch failed:", (fetchErr as Error).message);
        }
      }
    }

    const toOffer = (item: any, index: number) => ({
      id: String(item.id || item.url || index),
      store: item.site_name || item.store || item.domain || "Tienda Online",
      productName: item.title || item.productName || item.name || query,
      price: item.price || extractPrice(item.snippet || item.description || item.title) ||
        fetchPrices.get(item.url || item.sourceUrl) || "Ver en fuente",
      originalPrice: item.originalPrice || item.original_price || null,
      shipping: item.shipping || item.shipping_cost || "Consulta en tienda",
      delivery: item.delivery || item.delivery_estimate ||
        "Disponibilidad por confirmar",
      rating: String(item.rating ?? "—"),
      sourceUrl: item.url || item.sourceUrl || "https://example.com",
      summary: item.snippet || item.summary || item.description ||
        "Oferta encontrada en la web para " + query,
      tags: item.tags || ["Resultado web"],
      isBestValue: Boolean(item.isBestValue ?? item.is_best_value ?? false),
    });
    const offers = rawItems.length > 0
      ? rawItems.map(toOffer)
      : [
        toOffer({
          id: "1",
          store: "TinyFish Web Search",
          productName: `Resultado para: ${query}`,
          sourceUrl: `https://www.google.com/search?q=${encodedQuery}`,
          summary: `Mejor oferta y análisis web para la consulta: ${query}.`,
        }, 0),
      ];

    return json({
      query,
      offers,
      searchedAt: new Date().toISOString(),
    });
  } catch (error) {
    const err = error as Error;
    console.error("[search-offers] Error no controlado:", err.message, err.stack);
    return json({ error: err.message || "Internal Server Error" }, 500);
  }
});
