import { moneda } from '../lib/formato'

export default function PrecioCOP({ valor }: { valor: number }) {
  return <span>{moneda.format(valor)}</span>
}
